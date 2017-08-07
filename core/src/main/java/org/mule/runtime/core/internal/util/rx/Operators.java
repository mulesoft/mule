/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.util.ExceptionUtils.getRootCauseException;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

/**
 * Reusable operators to be use with project reactor.
 */
public final class Operators {

  private static Logger logger = getLogger(Operators.class);

  private Operators() {}

  /**
   * Custom function to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)} when a map function may return
   * {@code null} and this should be interpreted as empty rather than causing an error. If null is return by the function then the
   * {@link org.mule.runtime.core.api.EventContext} is also completed.
   * 
   * @param mapper map function
   * @return custom operator {@link BiConsumer} to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)}.
   */
  public static BiConsumer<Event, SynchronousSink<Event>> nullSafeMap(Function<Event, Event> mapper) {
    return (event, sink) -> {
      if (event != null) {
        Event result = mapper.apply(event);
        if (result != null) {
          sink.next(result);
        } else {
          event.getContext().success();
        }
      }
    };
  }

  /**
   * Return a singleton {@link Subscriber} that does not check for double onSubscribe and purely request Long.MAX. Unlike using
   * {@link Flux#subscribe()} directly this will not throw an exception if an error occurs.
   *
   * @return a new {@link Subscriber} whose sole purpose is to request Long.MAX
   */
  @SuppressWarnings("unchecked")
  public static <T> Subscriber<T> requestUnbounded() {
    return (Subscriber<T>) RequstMaxSubscriber.INSTANCE;
  }

  final static class RequstMaxSubscriber<T> implements Subscriber<T> {

    static final RequstMaxSubscriber INSTANCE = new RequstMaxSubscriber();

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {

    }

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onComplete() {

    }
  }

  /**
   * Register reactor hooks using the provided class. This needs to use reflection because reactor could be
   * loaded on different class loaders.
   *
   * In reactor 3.1, the error handler will be explicitly defined for each flux instead of globally.
   */
  public static void setupErrorHooks(Class clazz) {
    try {
      // Ensure reactor operatorError hook is always registered
      BiFunction<Throwable, Object, Throwable> onOperationErrorFunction = (throwable, signal) -> {
        // Unwrap all throwables to be consistent with reactor default hook.
        throwable = unwrap(throwable);
        // Only apply hook for Event signals.
        if (signal instanceof Event) {
          return throwable instanceof MessagingException ? throwable
              : new MessagingException((Event) signal, getRootCauseException(throwable));
        } else {
          return throwable;
        }
      };
      Method onOperatorError = clazz.getMethod("onOperatorError", BiFunction.class);
      onOperatorError.invoke(null, onOperationErrorFunction);

      // Log dropped events/errors rather than blow up which causes cryptic timeouts and stack traces
      Method onErrorDropped = clazz.getMethod("onErrorDropped", Consumer.class);
      Consumer<Object> onErrorDroppedFunction = error -> logger.error("ERROR DROPPED UNEXPECTEDLY " + error);
      onErrorDropped.invoke(null, onErrorDroppedFunction);

      Method onNextDropped = clazz.getMethod("onNextDropped", Consumer.class);
      Consumer<Object> onNextDroppedFunction = event -> logger.error("EVENT DROPPED UNEXPECTEDLY " + event);
      onNextDropped.invoke(null, onNextDroppedFunction);
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure reactor hooks", e);
    }
  }

}


