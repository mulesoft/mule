/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;


import static java.util.function.Function.identity;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryNotifier;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;


/**
 * A RetryPolicyTemplate creates a new {@link RetryPolicy} instance each time the retry goes into effect, thereby resetting any
 * state the policy may have (counters, etc.)
 * 
 * A {@link RetryNotifier} may be set in order to take action upon each retry attempt.
 */
public interface RetryPolicyTemplate {

  RetryPolicy createRetryInstance();

  Map<Object, Object> getMetaInfo();

  void setMetaInfo(Map<Object, Object> metaInfo);

  RetryNotifier getNotifier();

  void setNotifier(RetryNotifier retryNotifier);

  /**
   * Applies the retry policy by performing a blocking action.
   *
   * @param callback a callback with the logic to be executed on each retry
   * @param workManager the executor on which the retry operations are to be executed
   * @return a {@link RetryContext}
   */
  RetryContext execute(RetryCallback callback, Executor workManager) throws Exception;

  /**
   * Applies the retry policy in a non blocking manner by transforming the given {@code publisher} into one configured to apply
   * the retry logic.
   *
   * @param publisher a publisher with the items which might fail
   * @param <T> the generic type of the publisher's content
   * @return a {@link Publisher} configured with the retry policy.
   * @since 4.0
   */
  default <T> Publisher<T> applyPolicy(Publisher<T> publisher) {
    return createRetryInstance().applyPolicy(publisher, t -> true, t -> {
    }, identity());
  }

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
                                       Consumer<Throwable> onExhausted, Function<Throwable, Throwable> errorFunction) {
    return createRetryInstance().applyPolicy(publisher, shouldRetry, onExhausted, errorFunction);

  }
}
