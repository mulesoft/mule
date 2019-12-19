/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups an ordered list of {@link Interceptor interceptors} that will be applied as one.
 * <p>
 * It also contains based semantics and behaviors about the nuances of how should the {@link Interceptor} contract be
 * consumed.
 *
 * @since 4.3.0
 */
public interface InterceptorChain {

  /**
   * Builder for creating {@link InterceptorChain} instances
   */
  class Builder {

    private final List<Interceptor> interceptors = new ArrayList<>(2);

    private Builder() {
    }

    /**
     * Adds the given {@code interceptor} to the product chain. Interceptors will be executed in the order corresponding
     * to invocation of this method
     *
     * @param interceptor the interceptor to add
     * @return {@code this} instance
     */
    public Builder addInterceptor(Interceptor interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    /**
     * Builds and returns an {@link InterceptorChain}
     *
     * @return the product
     */
    public InterceptorChain build() {
      return interceptors.isEmpty() ? NullInterceptorChain.INSTANCE : LinkedInterceptorChain.of(interceptors);
    }
  }

  /**
   * @return a new {@link Builder}
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Executes the {@link Interceptor#before(ExecutionContext)} phase on each added interceptor.
   * <p>
   * Assuming a chain in which {@code 0 < N < M} , if interceptor {@code N} fails, then the
   * {@link Interceptor#after(ExecutionContext, Object)} method will be executed on N and all prior interceptors.
   * <p>
   * Additionally, {@link ExecutorCallback#error(Throwable)} is invoked on {@code callback} if it's not {@code null}.
   * {@link ExecutorCallback#complete(Object)} will never be invoked here.
   *
   * @param executionContext the {@link ExecutionContext}
   * @param callback         nullable callback in which errors are to be notified
   * @return {@code null} if all interceptors executed successfully or a {@link Throwable} if one of them fails
   */
  Throwable before(ExecutionContext executionContext, ExecutorCallback callback);

  /**
   * Executes the {@link Interceptor#onSuccess(ExecutionContext, Object)} phase on each added interceptor.
   * <p>
   * If case of an interceptor failing, the exception is logged and next interceptors are still executed. This method will
   * never fail
   *
   * @param executionContext the {@link ExecutionContext}
   * @param result           the operation's result
   */
  void onSuccess(ExecutionContext executionContext, Object result);

  /**
   * Executes the {@link Interceptor#onError(ExecutionContext, Throwable)} phase on each added interceptor.
   * <p>
   * Because each invocation {@link Interceptor#onError(ExecutionContext, Throwable)} returns a (potentially) mutated exception,
   * for each interceptor {@code N} in which {@code 0 < N < M}, the {@link Throwable} passed into it is the one that was returned
   * by interceptor {@code N -1}.
   * <p>
   * If case of an interceptor failing, the exception is logged and next interceptors are still executed. This method will
   * never fail
   * <p>
   * This method returns the {@link Throwable} returned by the last interceptor.
   *
   * @param executionContext the {@link ExecutionContext}
   * @param t                the exception found
   * @return the {@link Throwable} that this chain's consumer should handle.
   */
  Throwable onError(ExecutionContext executionContext, Throwable t);

  /**
   * Executes the {@link Interceptor#after(ExecutionContext, Object)} phase on each added interceptor, using {@code null} as
   * the second argument.
   * <p>
   * If case of an interceptor failing, the exception is logged and next interceptors are still executed. This method will
   * never fail.
   *
   * @param executionContext the {@link ExecutionContext}
   */
  void abort(ExecutionContext executionContext);
}
