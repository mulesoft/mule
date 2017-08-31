/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

/**
 * Exception indicating that retries have been exhausted after
 * {@link Retry#timeout(java.time.Duration)} or {@link Retry#retryMax(int)}.
 * For retries, {@link #getCause()} returns the original exception from the
 * last retry attempt that generated this exception.
 */
public class RetryExhaustedException extends RuntimeException {

  private static final long serialVersionUID = 6961442923363481283L;

  public RetryExhaustedException() {
    super();
  }

  public RetryExhaustedException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public RetryExhaustedException(String message, Throwable cause) {
    super(message, cause);
  }

  public RetryExhaustedException(String message) {
    super(message);
  }

  public RetryExhaustedException(Throwable cause) {
    super(cause);
  }
}
