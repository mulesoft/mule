/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessageRedeliveredException;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

//TODO: MULE-9307 re-write junits for rollback exception strategy


/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  private Integer maxRedeliveryAttempts;

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
  }

  public void setMaxRedeliveryAttempts(Integer maxRedeliveryAttempts) {
    this.maxRedeliveryAttempts = maxRedeliveryAttempts;
  }

  public Integer getMaxRedeliveryAttempts() {
    return maxRedeliveryAttempts;
  }

  public boolean hasMaxRedeliveryAttempts() {
    return this.maxRedeliveryAttempts != null;
  }

  @Override
  protected Function<InternalEvent, InternalEvent> beforeRouting(MessagingException exception) {
    return event -> {
      event = super.beforeRouting(exception).apply(event);
      if (!isRedeliveryExhausted(exception)) {
        rollback(exception);
      }
      return event;
    };
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return new ArrayList<>(super.getOwnedMessageProcessors());
  }

  private boolean isRedeliveryExhausted(Exception exception) {
    return (exception instanceof MessageRedeliveredException);
  }

  @Override
  protected Function<InternalEvent, Publisher<InternalEvent>> route(MessagingException exception) {
    if (isRedeliveryExhausted(exception)) {
      logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
    } else {
      return super.route(exception);
    }
    return event -> just(event);
  }

  @Override
  protected InternalEvent processReplyTo(InternalEvent event, Exception e) {
    if (isRedeliveryExhausted(e)) {
      return super.processReplyTo(event, e);
    } else {
      return event;
    }
  }

}
