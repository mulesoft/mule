/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * {@link InterceptorChain} implementation based on the Null Object pattern.
 *
 * @since 4.3.0
 */
class NullInterceptorChain implements InterceptorChain {

  static final InterceptorChain INSTANCE = new NullInterceptorChain();

  private NullInterceptorChain() {}

  @Override
  public Throwable before(ExecutionContext executionContext, ExecutorCallback callback) {
    return null;
  }

  @Override
  public void onSuccess(ExecutionContext executionContext, Object result) {}

  @Override
  public Throwable onError(ExecutionContext executionContext, Throwable t) {
    return t;
  }

  @Override
  public void abort(ExecutionContext executionContext) {}
}
