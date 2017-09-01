/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;

/**
 * Context provided to retry or repeat callbacks.
 *
 * @param <T> Application context type
 */
public interface Context<T> {

  /**
   * Application context that may be used to perform any rollbacks before
   * a retry. Application context can be configured using {@link Retry#withApplicationContext(Object)}
   * or {@link Repeat#withApplicationContext(Object)}.
   *
   * @return application context
   */
  public T applicationContext();

  /**
   * The next iteration number. This is a zero-based incrementing number with
   * the first attempt prior to any retries as iteration zero.
   * @return the current iteration number
   */
  public long iteration();

  /**
   * The backoff delay. When {@link Backoff} function is invoked, the previous
   * backoff is provided in the context. The context provided for the retry
   * predicates {@link Retry#onlyIf(java.util.function.Predicate)} and
   * {@link Repeat#onlyIf(java.util.function.Predicate)} as well as the retry
   * callbacks {@link Retry#doOnRetry(java.util.function.Consumer)} and
   * {@link Repeat#doOnRepeat(java.util.function.Consumer)} provide the
   * backoff delay for the next retry.
   *
   * @return Backoff delay
   */
  public Duration backoff();
}
