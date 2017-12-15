/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.PolicyNotificationHelper;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;

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

  @Inject
  private PolicyStateHandler policyStateHandler;

  @Inject
  private MuleContext muleContext;

  private PolicyNotificationHelper notificationHelper;

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
        .flatMapMany(event -> {
          Processor nextOperation = policyStateHandler.retrieveNextOperation(event.getContext().getCorrelationId());
          if (nextOperation == null) {
            return error(new MuleRuntimeException(createStaticMessage("There's no next operation configured for event context id "
                + event.getContext().getId())));
          }

          return just(event)
              .doOnNext(popBeforeNextFlowFlowStackElement().andThen(notificationHelper.notification(BEFORE_NEXT))
                  .andThen(req -> ((BaseEventContext) req.getContext())
                      .onResponse(notificationHelper.successOrErrorNotification(AFTER_NEXT))))
              .transform(nextOperation)
              .doOnSuccessOrError((ev, t) -> pushAfterNextFlowStackElement().accept(event));
        });
  }

  @Override
  public void initialise() throws InitialisationException {
    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(),
                                     this);
  }

}
