/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
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
  public Throwable before(ExecutionContext executionContext, CompletableComponentExecutor.ExecutorCallback callback) {
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
