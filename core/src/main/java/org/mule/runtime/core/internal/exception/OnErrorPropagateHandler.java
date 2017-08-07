/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessageRedeliveredException;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

//TODO: MULE-9307 re-write junits for rollback exception strategy


/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  private RedeliveryExceeded redeliveryExceeded;
  private Integer maxRedeliveryAttempts;


  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    initialiseIfNeeded(redeliveryExceeded);
    super.doInitialise(muleContext);
  }

  public void setRedeliveryExceeded(RedeliveryExceeded redeliveryExceeded) {
    this.redeliveryExceeded = redeliveryExceeded;
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
  protected Function<Event, Event> beforeRouting(MessagingException exception) {
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
    List<Processor> messageProcessors = new ArrayList<>(super.getOwnedMessageProcessors().size()
        + (redeliveryExceeded == null ? 0 : redeliveryExceeded.getMessageProcessors().size()));
    messageProcessors.addAll(super.getOwnedMessageProcessors());
    if (redeliveryExceeded != null) {
      messageProcessors.addAll(redeliveryExceeded.getMessageProcessors());
    }
    return messageProcessors;
  }

  private boolean isRedeliveryExhausted(Exception exception) {
    return (exception instanceof MessageRedeliveredException);
  }

  @Override
  protected Function<Event, Publisher<Event>> route(MessagingException exception) {
    if (isRedeliveryExhausted(exception)) {
      if (redeliveryExceeded != null) {
        markExceptionAsHandled(exception);
        return event -> just(event).transform(redeliveryExceeded);
      } else {
        logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
      }
    } else {
      return super.route(exception);
    }
    return event -> just(event);
  }

  @Override
  protected Event processReplyTo(Event event, Exception e) {
    if (isRedeliveryExhausted(e)) {
      return super.processReplyTo(event, e);
    } else {
      return event;
    }
  }

}
