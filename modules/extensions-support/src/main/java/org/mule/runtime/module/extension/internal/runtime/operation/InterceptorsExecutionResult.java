/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.List;

/**
 * Pojo that holds the result of the execution of a collection of interceptors.
 *
 * @since 4.0
 */
final class InterceptorsExecutionResult {

  private final Throwable throwable;
  private final List<Interceptor> executedInterceptors;

  public InterceptorsExecutionResult(Throwable throwable, List<Interceptor> executedInterceptors) {
    this.throwable = throwable;
    this.executedInterceptors = executedInterceptors;
  }

  /**
   * @return the {@link Throwable} that caused the execution failure
   */
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * @return a list of {@link Interceptor} that has been executed
   */
  public List<Interceptor> getExecutedInterceptors() {
    return executedInterceptors;
  }

  /**
   * @return boolean that represents if the execution finished correctly
   */
  public boolean isOk() {
    return throwable == null;
  }
}
