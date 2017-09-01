/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;

public class DefaultContext<T> implements RetryContext<T>, RepeatContext<T> {

  final T applicationContext;
  final long iteration;
  final Long repeatCompanionValue;
  final Throwable exception;
  final BackoffDelay backoff;
  BackoffDelay lastBackoff;

  public DefaultContext(T applicationContext,
                        long iteration,
                        BackoffDelay backoff,
                        long repeatCompanionValue) {
    this(applicationContext, iteration, backoff, repeatCompanionValue, null);
  }

  public DefaultContext(T applicationContext,
                        long iteration,
                        BackoffDelay backoff,
                        Throwable exception) {
    this(applicationContext, iteration, backoff, null, exception);
  }

  private DefaultContext(T applicationContext,
                         long iteration,
                         BackoffDelay backoff,
                         Long repeatCompanionValue,
                         Throwable exception) {
    this.applicationContext = applicationContext;
    this.iteration = iteration;
    this.backoff = backoff;
    this.repeatCompanionValue = repeatCompanionValue;
    this.exception = exception;
  }

  public T applicationContext() {
    return applicationContext;
  }

  public long iteration() {
    return iteration;
  }

  public Long companionValue() {
    return repeatCompanionValue;
  }

  public Throwable exception() {
    return exception;
  }

  public Duration backoff() {
    return backoff == null ? null : backoff.delay;
  }

  @Override
  public String toString() {
    if (exception != null)
      return String.format("iteration=%d exception=%s backoff=%s", iteration, exception, backoff);
    else
      return String.format("iteration=%d repeatCompanionValue=%s backoff=%s", iteration, repeatCompanionValue, backoff);
  }
}
