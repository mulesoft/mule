/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import java.util.Optional;

/**
 * POJO that holds the result of a Processor execution.
 *
 * @since 4.0
 */
final class OperationExecutionResult {

  private final Object output;
  private final Throwable exception;
  private final Optional<InterceptorsRetryRequest> retryRequest;

  public OperationExecutionResult(Object output, Throwable exception, Optional<InterceptorsRetryRequest> retryRequest) {
    this.output = output;
    this.exception = exception;
    this.retryRequest = retryRequest;
  }

  /**
   * @return the {@link Throwable} that caused the execution failure
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * @return the return of the operation
   */
  public Object getOutput() {
    return output;
  }

  /**
   * @return the {@link Optional} {@link InterceptorsRetryRequest} which contains if the interceptors asked to retry the request
   */
  public Optional<InterceptorsRetryRequest> getRetryRequest() {
    return retryRequest;
  }

  /**
   * @return boolean that represents if the execution finished correctly
   */
  public boolean isOk() {
    return exception == null;
  }
}
