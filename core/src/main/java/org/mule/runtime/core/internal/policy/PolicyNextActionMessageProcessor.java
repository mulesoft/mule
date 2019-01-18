/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

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

  @Inject
  private PolicyStateHandler policyStateHandler;

  @Inject
  private PolicyNextChaining policyNextChaining;

  @Inject
  private MuleContext muleContext;

  private PolicyNotificationHelper notificationHelper;

  private final PolicyEventConverter policyEventConverter = new PolicyEventConverter();

  private PolicyStateIdFactory stateIdFactory;

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
        .flatMap(event -> {
          PolicyStateId policyStateId = stateIdFactory.create(event);
          Processor nextOperation = policyNextChaining.retrieveNextOperation(policyStateId.getExecutionIdentifier());

          if (nextOperation == null) {
            return error(new MuleRuntimeException(createStaticMessage("There's no next operation configured for event context id "
                + policyStateId.getExecutionIdentifier())));
          }

          popBeforeNextFlowFlowStackElement().accept(event);
          notificationHelper.notification(BEFORE_NEXT).accept(event);

          return from(processWithChildContext(event, nextOperation, ofNullable(getLocation())))
              .doOnSuccessOrError(notificationHelper.successOrErrorNotification(AFTER_NEXT)
                  .andThen((ev, t) -> pushAfterNextFlowStackElement().accept(event)))
              .onErrorResume(MessagingException.class, t -> {

                policyStateHandler.getLatestState(policyStateId)
                    .ifPresent(latestStateEvent -> t.setProcessedEvent(policyEventConverter
                        .createEvent((PrivilegedEvent) t.getEvent(), (PrivilegedEvent) latestStateEvent)));

                // Given we've used child context to ensure AFTER_NEXT notifications are fired at exactly the right time we need
                // to propagate the error to parent context manually.
                ((BaseEventContext) event.getContext())
                    .error(resolveMessagingException(t.getFailingComponent(), muleContext).apply(t));
                return empty();
              })
              .doOnNext(coreEvent -> logExecuteNextEvent("After execute-next",
                                                         coreEvent.getContext(), coreEvent.getMessage(),
                                                         this.muleContext.getConfiguration().getId()));
        });
  }

  private Function<MessagingException, MessagingException> resolveMessagingException(Component processor,
                                                                                     MuleContext muleContext) {
    if (processor != null) {
      MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
      return exception -> exceptionResolver.resolve(exception, muleContext);
    } else {
      return exception -> exception;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(), this);
    stateIdFactory = new PolicyStateIdFactory(muleContext.getConfiguration().getId());
  }

  private void logExecuteNextEvent(String startingMessage, EventContext eventContext, Message message, String policyName) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("\nEvent Id: " + eventContext.getCorrelationId() + "\n" + startingMessage + ".\nPolicy: " + policyName
          + "\n" + message.getAttributes().getValue().toString());
    }
  }

}
