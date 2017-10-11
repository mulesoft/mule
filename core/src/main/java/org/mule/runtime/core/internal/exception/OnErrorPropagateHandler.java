/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.exception.MessageRedeliveredException;

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

  @Override
  public boolean acceptsAll() {
    return errorTypeMatcher == null && when == null;
  }

  @Override
  protected Function<CoreEvent, CoreEvent> beforeRouting(Exception exception) {
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
  protected Function<CoreEvent, Publisher<CoreEvent>> route(Exception exception) {
    if (isRedeliveryExhausted(exception)) {
      logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
    } else {
      return super.route(exception);
    }
    return event -> just(event);
  }

  @Override
  protected CoreEvent processReplyTo(CoreEvent event, Exception e) {
    if (isRedeliveryExhausted(e)) {
      return super.processReplyTo(event, e);
    } else {
      return event;
    }
  }

}
