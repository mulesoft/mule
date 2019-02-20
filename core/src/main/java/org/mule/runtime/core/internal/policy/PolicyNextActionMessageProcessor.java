/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.policy.OperationPolicyProcessor.POLICY_OPERATION_ORIGINAL_EVENT;
import static org.mule.runtime.core.internal.policy.SourcePolicyProcessor.POLICY_SOURCE_ORIGINAL_EVENT;
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
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

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
  public static final String POLICY_STATE_EVENT = "policy.beforeNextEvent";
  public static final String POLICY_NEXT_EVENT_CTX_IDS = "policy.next.eventCtxIds";
  public static final String POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS = "policy.isPropagateMessageTransformations";

  @Inject
  private MuleContext muleContext;

  private PolicyNotificationHelper notificationHelper;

  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
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

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(coreEvent -> logExecuteNextEvent("Before execute-next", coreEvent.getContext(),
                                                   coreEvent.getMessage(), muleContext.getConfiguration().getId()))
        .flatMap(event -> subscriberContext()
            .flatMap(ctx -> Mono.just(addEventContextHandledByThisNext(
                                                                       ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)
                                                                           ? policyEventConverter
                                                                               .createEvent(saveState((PrivilegedEvent) event),
                                                                                            getOriginalEvent(event),
                                                                                            ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS))
                                                                           : policyEventConverter
                                                                               .createEvent(saveState((PrivilegedEvent) event),
                                                                                            getOriginalEvent(event))))))
        .doOnNext(event -> {
          popBeforeNextFlowFlowStackElement().accept(event);
          notificationHelper.notification(BEFORE_NEXT).accept(event);
        })
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.transform(ctx.get(POLICY_NEXT_OPERATION)).cast(CoreEvent.class)))
        .doOnNext(coreEvent -> {
          notificationHelper.fireNotification(coreEvent, null, AFTER_NEXT);
          pushAfterNextFlowStackElement().accept(coreEvent);
          logExecuteNextEvent("After execute-next", coreEvent.getContext(), coreEvent.getMessage(),
                              this.muleContext.getConfiguration().getId());
        })
        .map(result -> (CoreEvent) policyEventConverter.createEvent((PrivilegedEvent) result,
                                                                    loadState((PrivilegedEvent) result)))
        .onErrorContinue(MessagingException.class, (error, ev) -> {
          final CoreEvent event = ((MessagingException) error).getEvent();

          if (isEventContextHandledByThisNext(event)) {
            MessagingException me = (MessagingException) error;
            notificationHelper.fireNotification(me.getEvent(), me, AFTER_NEXT);
            pushAfterNextFlowStackElement().accept(me.getEvent());

            me.setProcessedEvent(policyEventConverter.createEvent((PrivilegedEvent) me.getEvent(),
                                                                  loadState((PrivilegedEvent) me.getEvent())));

            ((BaseEventContext) event.getContext()).error(error);
          }
        });
  }

  private CoreEvent addEventContextHandledByThisNext(CoreEvent event) {
    final Set<String> eventCtxIds = ((InternalEvent) event).getInternalParameter(POLICY_NEXT_EVENT_CTX_IDS);

    return quickCopy(event, singletonMap(POLICY_NEXT_EVENT_CTX_IDS, eventCtxIds == null
        ? singleton(event.getContext().getId())
        : ImmutableSet.builder().addAll(eventCtxIds).add(event.getContext().getId()).build()));
  }

  private boolean isEventContextHandledByThisNext(CoreEvent event) {
    final Set<String> eventCtxIds = ((InternalEvent) event).getInternalParameter(POLICY_NEXT_EVENT_CTX_IDS);
    return eventCtxIds != null && eventCtxIds.contains(event.getContext().getId());
  }

  private PrivilegedEvent getOriginalEvent(CoreEvent event) {
    final PrivilegedEvent operationOriginalEvent =
        ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_ORIGINAL_EVENT);
    if (operationOriginalEvent != null) {
      return operationOriginalEvent;
    } else {
      return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_ORIGINAL_EVENT);
    }
  }

  private PrivilegedEvent saveState(PrivilegedEvent event) {
    return quickCopy(event, singletonMap(POLICY_STATE_EVENT, event));
  }

  private PrivilegedEvent loadState(PrivilegedEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_STATE_EVENT);
  }

  @Override
  public void initialise() throws InitialisationException {
    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(), this);
  }

  private void logExecuteNextEvent(String startingMessage, EventContext eventContext, Message message, String policyName) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("\nEvent Id: " + eventContext.getCorrelationId() + "\n" + startingMessage + ".\nPolicy: " + policyName
          + "\n" + message.getAttributes().getValue().toString());
    }
  }
}
