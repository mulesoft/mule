/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

class NullInterceptorChain implements InterceptorChain {

  static final InterceptorChain INSTANCE = new NullInterceptorChain();

  private NullInterceptorChain() {}

  @Override
  public Throwable before(ExecutionContextAdapter executionContext, CompletableComponentExecutor.ExecutorCallback callback) {
    return null;
  }

  @Override
  public void onSuccess(ExecutionContextAdapter executionContext, Object result) {

  }

  @Override
  public Throwable onError(ExecutionContextAdapter executionContext, Throwable t) {
    return t;
  }
}
