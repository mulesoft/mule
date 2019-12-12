/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.core.api.event.CoreEvent.builder;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

/**
 * Reusable operators to be use with project reactor.
 */
public final class Operators {

  private Operators() {
  }

  /**
   * Custom function to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)} when a map function may return
   * {@code null} and this should be interpreted as empty rather than causing an error. If null is return by the function then the
   * {@link BaseEventContext} is also completed.
   *
   * @param mapper map function
   * @return custom operator {@link BiConsumer} to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)}.
   */
  public static BiConsumer<CoreEvent, SynchronousSink<CoreEvent>> nullSafeMap(Function<CoreEvent, CoreEvent> mapper) {
    return (event, sink) -> {
      try {
        if (event != null) {
          CoreEvent result = mapper.apply(event);
          if (result != null) {
            sink.next(result);
          } else {
            ((BaseEventContext) event.getContext()).success();
          }
        }
      } catch (Exception e) {
        sink.error(e);
      }
    };
  }

  @Deprecated
  public static Function<CoreEvent, CoreEvent> outputToTarget(CoreEvent originalEvent, String target,
                                                              String targetValueExpression,
                                                              ExpressionLanguage expressionManager) {
    return result -> {
      if (target != null) {
        TypedValue targetValue = expressionManager.evaluate(targetValueExpression, getTargetBindingContext(result.getMessage()));
        return builder(originalEvent).addVariable(target, targetValue.getValue(), targetValue.getDataType()).build();
      } else {
        return result;
      }
    };
  }


  public static CoreEvent outputToTarget(CoreEvent originalEvent,
                                         CoreEvent result,
                                         String target,
                                         CompiledExpression targetValueExpression,
                                         ExpressionLanguage expressionLanguage) {
    if (target != null) {
      try (ExpressionLanguageSession session = expressionLanguage.openSession(getTargetBindingContext(result.getMessage()))) {
        TypedValue targetValue = session.evaluate(targetValueExpression);
        return builder(originalEvent)
            .addVariable(target, targetValue.getValue(), targetValue.getDataType())
            .build();
      }
    } else {
      return result;
    }
  }

  /**
   * Return a singleton {@link Subscriber} that does not check for double onSubscribe and purely request Long.MAX. Unlike using
   * {@link Flux#subscribe()} directly this will not throw an exception if an error occurs.
   *
   * @return a new {@link Subscriber} whose sole purpose is to request Long.MAX
   */
  @SuppressWarnings("unchecked")
  public static <T> Subscriber<T> requestUnbounded() {
    return RequestMaxSubscriber.INSTANCE;
  }

  final static class RequestMaxSubscriber<T> implements Subscriber<T> {

    static final RequestMaxSubscriber INSTANCE = new RequestMaxSubscriber();

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {

    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onComplete() {

    }
  }
}


