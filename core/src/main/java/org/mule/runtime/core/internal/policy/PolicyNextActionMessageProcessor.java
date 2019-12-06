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
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.BaseExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * Next-operation message processor implementation.
 *
 * Such implementation handles a set of callbacks to execute as next operations that are must be configured before processing the
 * event.
 *
 * @since 4.0
 */
public class PolicyNextActionMessageProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final String SOURCE_POLICY_PART_IDENTIFIER = "source";

  private static final Logger LOGGER = getLogger(PolicyNextActionMessageProcessor.class);

  public static final String POLICY_NEXT_OPERATION = "policy.nextOperation";
  public static final String POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS = "policy.isPropagateMessageTransformations";

  @Inject
  private MuleContext muleContext;

  @Inject
  private ServerNotificationHandler notificationManager;

  private PolicyNotificationHelper notificationHelper;
  private PolicyEventMapper policyEventMapper;

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

    final Function<CoreEvent, CoreEvent> prepareEvent = policyEventMapper::fromPolicyNext;

    // if current execute-next belongs to a `source` policy
    if (getLocation().getParts().get(1).getPartIdentifier()
        .map(tci -> tci.getIdentifier().getName().equals(SOURCE_POLICY_PART_IDENTIFIER))
        .orElse(false)) {
      this.onExecuteNextErrorConsumer = new OnExecuteNextErrorConsumer(prepareEvent.andThen(event -> policyEventMapper
          .onFlowError(event, getPolicyId(), SourcePolicyContext.from(event).getParametersTransformer())),
                                                                       notificationHelper, getLocation());
    } else {
      this.onExecuteNextErrorConsumer =
          new OnExecuteNextErrorConsumer(prepareEvent, notificationHelper, getLocation());
    }

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
                .transform(ctx.get(POLICY_NEXT_OPERATION)));
      }
    }), policyNextErrorHandler());
    initialiseIfNeeded(nextDispatchAsChain, muleContext);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(coreEvent -> logExecuteNextEvent("Before execute-next", coreEvent.getContext(),
                                                   coreEvent.getMessage(), getPolicyId()))
        .doOnNext(event -> {
          popBeforeNextFlowFlowStackElement().accept(event);
          notificationHelper.notification(BEFORE_NEXT).accept(event);
        })
        .compose(nextDispatchAsChain)
        .doOnNext(coreEvent -> {
          notificationHelper.fireNotification(coreEvent, null, AFTER_NEXT);
          pushAfterNextFlowStackElement().accept(coreEvent);
          logExecuteNextEvent("After execute-next", coreEvent.getContext(), coreEvent.getMessage(),
                              getPolicyId());
        })
        .map(policyEventMapper::fromPolicyNext);
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

  private String toPolicyLocation(ComponentLocation componentLocation) {
    return componentLocation.getParts().get(0).getPartPath() + "/" + componentLocation.getParts().get(1).getPartPath()
        + "[after next]";
  }

  private Consumer<CoreEvent> popBeforeNextFlowFlowStackElement() {
    return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
  }

  private String getPolicyId() {
    return muleContext.getConfiguration().getId();
  }

  private void logExecuteNextEvent(String startingMessage, EventContext eventContext, Message message, String policyName) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("\nEvent Id: " + eventContext.getCorrelationId() + "\n" + startingMessage + ".\nPolicy: " + policyName
          + "\n" + message.getAttributes().getValue().toString());
    }
  }
}
