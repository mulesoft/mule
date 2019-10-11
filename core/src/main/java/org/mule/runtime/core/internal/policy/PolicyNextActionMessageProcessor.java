/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
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
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.function.Consumer;

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

  private static final Logger LOGGER = getLogger(PolicyNextActionMessageProcessor.class);

  public static final String POLICY_NEXT_OPERATION = "policy.nextOperation";
  public static final String POLICY_NEXT_EVENT_CTX_IDS = "policy.next.eventCtxIds";
  public static final String POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS = "policy.isPropagateMessageTransformations";

  @Inject
  private MuleContext muleContext;

  private PolicyNotificationHelper notificationHelper;
  private PolicyEventMapper policyEventMapper;

  private OnExecuteNextErrorConsumer onExecuteNextErrorConsumer;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public void initialise() throws InitialisationException {
    this.policyEventMapper = new PolicyEventMapper(getPolicyId());
    this.notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), getPolicyId(), this);
    this.onExecuteNextErrorConsumer =
        new OnExecuteNextErrorConsumer(policyEventMapper::fromPolicyNext, notificationHelper, getLocation());
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
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub
                .map(event -> ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)
                    ? policyEventMapper.onSourcePolicyNext(event, ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS))
                    : policyEventMapper.onOperationPolicyNext(event))
                .transform(ctx.get(POLICY_NEXT_OPERATION))
                .cast(CoreEvent.class)))
        .doOnNext(coreEvent -> {
          notificationHelper.fireNotification(coreEvent, null, AFTER_NEXT);
          pushAfterNextFlowStackElement().accept(coreEvent);
          logExecuteNextEvent("After execute-next", coreEvent.getContext(), coreEvent.getMessage(),
                              getPolicyId());
        })
        .map(policyEventMapper::fromPolicyNext)
        .onErrorContinue(MessagingException.class, (error, v) -> onExecuteNextErrorConsumer.accept(error));
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
