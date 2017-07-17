/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

/**
 * A RetryPolicy takes some action each time an exception occurs and returns a {@link PolicyStatus} which indicates whether the
 * policy is exhausted or should continue to retry.
 */
public interface RetryPolicy {

  /**
   * Applies the retry policy by performing a blocking action.
   *
   * @param cause the failure which causes the retry
   * @return a {@link PolicyStatus}
   */
  PolicyStatus applyPolicy(Throwable cause);


  /**
   * Applies the retry policy in a non blocking manner by transforming the given {@code publisher} into one configured to apply
   * the retry logic.
   *
   * @param publisher a publisher with the items which might fail
   * @param shouldRetry a predicate which evaluates each item to know if it should be retried or not
   * @param onExhausted an action to perform when the retry action has been exhausted
   * @param errorFunction function used to map cause exception to exception emitted by retry policy.
   * @param <T> the generic type of the publisher's content
   * @return a {@link Publisher} configured with the retry policy.
   * @since 4.0
   */
  default <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                       Predicate<Throwable> shouldRetry,
                                       Consumer<Throwable> onExhausted,
                                       Function<Throwable, Throwable> errorFunction) {
    return publisher;
  }

}
