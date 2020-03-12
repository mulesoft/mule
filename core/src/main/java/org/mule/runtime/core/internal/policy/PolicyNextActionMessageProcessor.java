/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.BaseExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.lang.ref.Reference;
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
public class PolicyNextActionMessageProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final String SOURCE_POLICY_PART_IDENTIFIER = "source";

  public static final String POLICY_NEXT_OPERATION = "policy.nextOperation";
  public static final String POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS = "policy.isPropagateMessageTransformations";

  @Inject
  private MuleContext muleContext;

  @Inject
  private ServerNotificationHandler notificationManager;

  private PolicyNotificationHelper notificationHelper;
  private PolicyEventMapper policyEventMapper;
  private PolicyTraceLogger policyTraceLogger = new PolicyTraceLogger();

  private OnExecuteNextErrorConsumer onExecuteNextErrorConsumer;

  private MessageProcessorChain nextDispatchAsChain;

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
        return subscriberContext()
            .flatMapMany(ctx -> from(eventPub)
                .map(event -> ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)
                    ? policyEventMapper.onSourcePolicyNext(event, ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS))
                    : policyEventMapper.onOperationPolicyNext(event))
                .transform((ReactiveProcessor) ((Reference) ctx.get(POLICY_NEXT_OPERATION)).get()));
      }
    }), policyNextErrorHandler());
    initialiseIfNeeded(nextDispatchAsChain, muleContext);
  }

  private OnExecuteNextErrorConsumer errorConsumer(PolicyEventMapper policyEventMapper,
                                                   PolicyNotificationHelper notificationHelper) {

    if (isWithinSourcePolicy(getLocation())) {
      return new OnExecuteNextErrorConsumer(me -> {
        final CoreEvent event = me.getEvent();

        // for backpressure errors, the MessagingException does not have the failingComponent set
        if (me.getFailingComponent() == null ||
            isWithinSourcePolicy(me.getFailingComponent().getLocation())) {
          return policyEventMapper.fromPolicyNext(event);
        } else {
          return policyEventMapper.onFlowError(policyEventMapper.fromPolicyNext(event), getPolicyId(),
                                               SourcePolicyContext.from(event).getParametersTransformer());
        }
      }, notificationHelper, getLocation());
    } else {
      return new OnExecuteNextErrorConsumer(me -> policyEventMapper.fromPolicyNext(me.getEvent()), notificationHelper,
                                            getLocation());
    }
  }

  private Boolean isWithinSourcePolicy(final ComponentLocation loc) {
    return loc.getParts().get(1).getPartIdentifier()
        .map(tci -> tci.getIdentifier().getName().equals(SOURCE_POLICY_PART_IDENTIFIER))
        .orElse(false);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(event -> policyTraceLogger.logBeforeExecuteNext(getPolicyId(), event))
        .doOnNext(event -> {
          popBeforeNextFlowFlowStackElement().accept(event);
          notificationHelper.notification(BEFORE_NEXT).accept(event);
        })
        .compose(nextDispatchAsChain)
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
        .push(new FlowStackElement(toPolicyLocation(getLocation()), null));
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

}
