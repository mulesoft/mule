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

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;

import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
 */
public interface MessagingExceptionHandler extends ExceptionHandler, Function<MessagingException, Publisher<Event>> {

  /**
   * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
   * 
   * @param exception which occurred
   * @param event which was being processed when the exception occurred
   * @return new event to route on to the rest of the flow, generally with ExceptionPayload set on the message
   */
  Event handleException(MessagingException exception, Event event);

  @Override
  default Publisher<Event> apply(MessagingException exception) {
    try {
      exception.setProcessedEvent(handleException(exception, exception.getEvent()));
      if (exception.handled()) {
        return just(exception.getEvent());
      } else {
        return error(exception);
      }
    } catch (Throwable throwable) {
      return error(propagateWrappingFatal(throwable));
    }
  }
}


