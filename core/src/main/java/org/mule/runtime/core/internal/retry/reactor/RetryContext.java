/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

/**
 * Context provided to retry predicate {@link Retry#onlyIf(java.util.function.Predicate)} and
 * the retry callback {@link Retry#doOnRetry(java.util.function.Consumer)}.
 *
 * @param <T> Application context type
 */
public interface RetryContext<T> extends Context<T> {

  /**
   * Returns the exception from the last iteration.
   * @return exception that resulted in retry
   */
  public Throwable exception();
}
