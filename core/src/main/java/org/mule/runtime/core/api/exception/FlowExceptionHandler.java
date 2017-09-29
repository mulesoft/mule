/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

import org.reactivestreams.Publisher;

import java.util.function.Function;

/**
 * Take some action when an exception has occurred while executing a Flow for an event.
 */
public interface FlowExceptionHandler extends Function<Exception, Publisher<CoreEvent>> {

  /**
   * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
   *
   * @param exception which occurred
   * @param event which was being processed when the exception occurred
   * @return new event to route on to the rest of the flow, generally with ExceptionPayload set on the message
   */
  CoreEvent handleException(Exception exception, CoreEvent event);

  @Override
  default Publisher<CoreEvent> apply(Exception exception) {
    try {
      if (exception instanceof MessagingException) {
        MessagingException me = (MessagingException) exception;
        me.setProcessedEvent(handleException(exception, me.getEvent()));
        if (me.handled()) {
          return just(me.getEvent());
        } else {
          return error(exception);
        }
      } else {
        return error(exception);
      }
    } catch (Throwable throwable) {
      return error(propagateWrappingFatal(throwable));
    }
  }
}

