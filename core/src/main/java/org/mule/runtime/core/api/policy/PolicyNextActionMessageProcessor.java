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
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.PolicyNotificationHelper;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

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
              .doOnNext(notificationHelper.notification(BEFORE_NEXT))
              .transform(nextOperation)
              .doOnSuccess(notificationHelper.notification(AFTER_NEXT))
              .doOnError(MessagingException.class, notificationHelper.errorNotification(AFTER_NEXT));
        });
  }

  @Override
  public void initialise() throws InitialisationException {
    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(),
                                     this);
  }

}
