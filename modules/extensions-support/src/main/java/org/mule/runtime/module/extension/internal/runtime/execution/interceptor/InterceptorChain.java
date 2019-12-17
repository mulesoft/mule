/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.ArrayList;
import java.util.List;

public interface InterceptorChain {

  class Builder {

    private final List<Interceptor> interceptors = new ArrayList<>(2);

    private Builder() {}

    public Builder addInterceptor(Interceptor interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    public InterceptorChain build() {
      return interceptors.isEmpty() ? NullInterceptorChain.INSTANCE : DefaultInterceptorChain.of(interceptors);
    }
  }

  static Builder builder() {
    return new Builder();
  }

  Throwable before(ExecutionContextAdapter executionContext, CompletableComponentExecutor.ExecutorCallback callback);

  void onSuccess(ExecutionContextAdapter executionContext, Object result);

  Throwable onError(ExecutionContextAdapter executionContext, Throwable t);
}
