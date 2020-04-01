/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static reactor.core.publisher.Mono.from;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * A RetryPolicy takes some action each time an exception occurs and returns a {@link PolicyStatus} which indicates whether the
 * policy is exhausted or should continue to retry.
 */
@NoImplement
public interface RetryPolicy {

  /**
   * Applies the retry policy by performing a blocking action.
   *
   * @param cause the failure which causes the retry
   * @return a {@link PolicyStatus}
   */
  PolicyStatus applyPolicy(Throwable cause);


  default <T> CompletableFuture<T> applyPolicy(Supplier<CompletableFuture<T>> futureSupplier,
                                               Predicate<Throwable> shouldRetry,
                                               Consumer<Throwable> onRetry,
                                               Consumer<Throwable> onExhausted,
                                               Function<Throwable, Throwable> errorFunction,
                                               Scheduler retryScheduler) {

    CompletableFuture<T> completedFuture = new CompletableFuture<>();
    try {
      CompletableFuture<T> retry = futureSupplier.get();
      retry.whenComplete((v, e) -> {
        if (e != null) {
          try {
            e = errorFunction.apply(unwrap(e));
            onExhausted.accept(e);
          } finally {
            completedFuture.completeExceptionally(e);
          }
        } else {
          completedFuture.complete(v);
        }
      });
    } catch (Throwable t) {
      try {
        t = errorFunction.apply(unwrap(t));
        onExhausted.accept(t);
      } finally {
        completedFuture.completeExceptionally(t);
      }
    }

    return completedFuture;
  }

  /**
   * Applies the retry policy in a non blocking manner by transforming the given {@code publisher} into one configured to apply
   * the retry logic.
   *
   * @param publisher     a publisher with the items which might fail
   * @param shouldRetry   a predicate which evaluates each item to know if it should be retried or not
   * @param onExhausted   an action to perform when the retry action has been exhausted
   * @param errorFunction function used to map cause exception to exception emitted by retry policy.
   * @param <T>           the generic type of the publisher's content
   * @return a {@link Publisher} configured with the retry policy.
   * @since 4.0
   * @deprecated Use {@link #applyPolicy(Publisher, Predicate, Consumer, Function, Scheduler)} instead
   */
  @Deprecated
  default <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                       Predicate<Throwable> shouldRetry,
                                       Consumer<Throwable> onExhausted,
                                       Function<Throwable, Throwable> errorFunction) {
    return applyPolicy(publisher, shouldRetry, onExhausted, errorFunction, IMMEDIATE_SCHEDULER);
  }

  /**
   * Applies the retry policy in a non blocking manner by transforming the given {@code publisher} into one configured to apply
   * the retry logic.
   *
   * @param publisher      a publisher with the items which might fail
   * @param shouldRetry    a predicate which evaluates each item to know if it should be retried or not
   * @param onExhausted    an action to perform when the retry action has been exhausted
   * @param errorFunction  function used to map cause exception to exception emitted by retry policy.
   * @param retryScheduler the scheduler to use when retrying. If empty, an internal reactor Scheduler will be used.
   * @param <T>            the generic type of the publisher's content
   * @return a {@link Publisher} configured with the retry policy.
   * @since 4.2
   */
  default <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                       Predicate<Throwable> shouldRetry,
                                       Consumer<Throwable> onExhausted,
                                       Function<Throwable, Throwable> errorFunction,
                                       Scheduler retryScheduler) {
    return from(publisher).onErrorMap(e -> {
      e = unwrap(e);
      onExhausted.accept(e);
      return errorFunction.apply(e);
    });
  }
}
