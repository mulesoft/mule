/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_NEXT_ACTION_SPAN_NAME;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.deferContextual;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.BaseExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.event.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Next-operation message processor implementation.
 * <p>
 * Such implementation handles a set of callbacks to execute as next operations that are must be configured before processing the
 * event.
 *
 * @since 4.0
 */
public class PolicyNextActionMessageProcessor extends AbstractComponent implements Processor, Initialisable, Disposable {

  public static final String EXECUTE_NEXT = "execute-next";

  static final String SOURCE_POLICY_PART_IDENTIFIER = "source";
  static final String SUBFLOW_POLICY_PART_IDENTIFIER = "sub-flow";

  public static final String POLICY_NEXT_OPERATION = "policy.nextOperation";
  public static final String POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS = "policy.isPropagateMessageTransformations";

  @Inject
  private MuleContext muleContext;

  @Inject
  private ServerNotificationHandler notificationManager;

  @Inject
  private ComponentTracerFactory componentTracerFactory;

  private PolicyNotificationHelper notificationHelper;
  private PolicyEventMapper policyEventMapper;
  private final PolicyTraceLogger policyTraceLogger = new PolicyTraceLogger();

  private OnExecuteNextErrorConsumer onExecuteNextErrorConsumer;

  private MessageProcessorChain nextDispatchAsChain;

  private final Map<ComponentLocation, Boolean> locationsCache = new SmallMap<>();
  private final Map<Pair<ComponentLocation, FlowStackElement>, Boolean> subFlowLocationsCache = new SmallMap<>();

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public void initialise() throws InitialisationException {
    this.policyEventMapper = new PolicyEventMapper(getPolicyId());
    this.notificationHelper = new PolicyNotificationHelper(notificationManager, getPolicyId(), this);

    this.onExecuteNextErrorConsumer = errorConsumer(this.policyEventMapper, this.notificationHelper);

    // this chain exists only so that an error handler can be hooked to map the event in the propagated error.
    this.nextDispatchAsChain = buildNewChainWithListOfProcessors(empty(), singletonList(new Processor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPub) {
        return deferContextual(ctx -> from(eventPub)
            .map(event -> ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)
                ? policyEventMapper.onSourcePolicyNext(event, ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS))
                : policyEventMapper.onOperationPolicyNext(event))
            .transform((ReactiveProcessor) ((Reference) ctx.get(POLICY_NEXT_OPERATION)).get()));
      }
    }), policyNextErrorHandler(), componentTracerFactory.fromComponent(this, POLICY_NEXT_ACTION_SPAN_NAME, ""));
    initialiseIfNeeded(nextDispatchAsChain, muleContext);
  }

  private OnExecuteNextErrorConsumer errorConsumer(PolicyEventMapper policyEventMapper,
                                                   PolicyNotificationHelper notificationHelper) {

    return new OnExecuteNextErrorConsumer(me -> {
      if (!isWithinSourcePolicy(getLocation()) && !isWithingSubflowInSourcePolicy(getLocation(), me)) {
        return policyEventMapper.fromPolicyNext(me.getEvent());
      }
      final CoreEvent event = me.getEvent();

      if (me.getFailingComponent() != null && (isWithinSourcePolicy(me.getFailingComponent().getLocation())
          || isWithingSubflowInSourcePolicy(me.getFailingComponent().getLocation(), me))) {
        return policyEventMapper.fromPolicyNext(event);
      } else {
        return policyEventMapper.fromPolicyNext(policyEventMapper
            .onFlowError(event, getPolicyId(), SourcePolicyContext.from(event).getParametersTransformer()));
      }

    }, notificationHelper, getLocation(), getAnnotations());
  }

  private Boolean isWithinSourcePolicy(final ComponentLocation location) {
    return locationsCache.computeIfAbsent(location, loc -> loc.getParts().size() >= 2 && loc.getParts().get(1).getPartIdentifier()
        .map(tci -> tci.getIdentifier().getName().equals(SOURCE_POLICY_PART_IDENTIFIER)).orElse(false));
  }

  private Boolean isWithingSubflowInSourcePolicy(ComponentLocation loc, MessagingException me) {
    List<FlowStackElement> elements = me.getEvent().getFlowCallStack().getElements();
    if (elements.size() == 0) {
      return false;
    }
    return subFlowLocationsCache.computeIfAbsent(new Pair<>(loc, elements.get(0)), pair -> {
      if (pair.getFirst().getParts().size() < 1) {
        return false;
      }
      boolean isWithinSubflow = pair.getFirst().getParts().get(0).getPartIdentifier()
          .map(tci -> tci.getIdentifier().getName().equals(SUBFLOW_POLICY_PART_IDENTIFIER)).orElse(false);
      if (!isWithinSubflow) {
        return false;
      }

      return isSubflowWithinASoucePolicy(builderFromStringRepresentation(pair.getSecond().executingLocation().getLocation())
          .build());
    });
  }

  private boolean isSubflowWithinASoucePolicy(Location loc) {
    List<String> parts = loc.getParts();
    return loc.getGlobalName().equals(getRootContainerLocation().toString()) && parts.size() >= 1
        && parts.get(0).equals(SOURCE_POLICY_PART_IDENTIFIER);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(event -> policyTraceLogger.logBeforeExecuteNext(getPolicyId(), event))
        .doOnNext(event -> {
          popBeforeNextFlowFlowStackElement().accept(event);
          notificationHelper.notification(BEFORE_NEXT).accept(event);
        })
        .transformDeferred(nextDispatchAsChain)
        .doOnNext(coreEvent -> {
          notificationHelper.fireNotification(coreEvent, null, AFTER_NEXT);
          pushAfterNextFlowStackElement().accept(coreEvent);
        })
        .map(policyEventMapper::fromPolicyNext)
        .doOnNext(event -> policyTraceLogger.logAfterExecuteNext(getPolicyId(), event));
  }

  private BaseExceptionHandler policyNextErrorHandler() {
    return new BaseExceptionHandler() {

      @Override
      public void onError(Exception error) {
        onExecuteNextErrorConsumer.accept(error);
      }

      @Override
      public String toString() {
        return PolicyNextActionMessageProcessor.class.getSimpleName() + ".errorHandler @ " + getLocation().getLocation();
      }
    };
  }

  private Consumer<CoreEvent> pushAfterNextFlowStackElement() {
    return event -> ((DefaultFlowCallStack) event.getFlowCallStack())
        .push(new FlowStackElement(toPolicyLocation(getLocation()), null, getLocation(), getAnnotations()));
  }

  private static String toPolicyLocation(ComponentLocation componentLocation) {
    return componentLocation.getParts().get(0).getPartPath() + "/" + componentLocation.getParts().get(1).getPartPath()
        + "[after next]";
  }

  private static Consumer<CoreEvent> popBeforeNextFlowFlowStackElement() {
    return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
  }

  private String getPolicyId() {
    return muleContext.getConfiguration().getId();
  }

  @Override
  public void dispose() {
    disposeIfNeeded(nextDispatchAsChain, getLogger(getClass()));
  }
}
