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

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Take some action when an exception has occurred while executing a Flow for an event.
 */
@NoImplement
public interface FlowExceptionHandler extends Function<Exception, Publisher<CoreEvent>> {

  /**
   * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
   *
   * @param exception which occurred
   * @param event which was being processed when the exception occurred
   * @return new event to route on to the rest of the flow, generally with ExceptionPayload set on the message
   * @deprecated Use {@link FlowExceptionHandler#routeError(Exception, Consumer, Consumer)}
   */
  @Deprecated
  CoreEvent handleException(Exception exception, CoreEvent event);

  /**
   * @param exception the exception to handle
   * @return the publisher with the handling result
   * @deprecated Use {@link FlowExceptionHandler#routeError(Exception, Consumer, Consumer)}
   */
  @Override
  @Deprecated
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

  /**
   * Routes the error towards the destination error handler, calling the corresponding callback in case of failure or success.
   *
   * @param error the {@link Exception} to route
   * @param continueCallback the callback called in case the error is successfully handled
   * @param propagateCallback the callback is called in case the error-handling fails
   *
   * @since 4.3
   */
  default void routeError(Exception error, Consumer<CoreEvent> continueCallback,
                          Consumer<Throwable> propagateCallback) {
    propagateCallback.accept(error);
  }
}

