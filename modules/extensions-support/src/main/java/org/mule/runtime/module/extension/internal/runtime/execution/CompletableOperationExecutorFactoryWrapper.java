/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Collections.emptyMap;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Decorates a {@link CompletableComponentExecutorFactory} so that the instances that it generates are hooked into our non
 * blocking execution engine and decorated through {@link InterceptableOperationExecutorWrapper}, so that the items in the
 * {@link #interceptors} list can apply.
 *
 * @since 4.2
 */
public final class CompletableOperationExecutorFactoryWrapper<T extends ComponentModel>
    implements CompletableComponentExecutorFactory<T>, OperationArgumentResolverFactory<T> {

  private final CompletableComponentExecutorFactory delegate;
  private final List<Interceptor> interceptors;

  /**
   * Creates a new instance
   *
   * @param delegate     the {@link CompletableComponentExecutorFactory} to be decorated
   * @param interceptors a {@link List} with the {@link Interceptor interceptors} that should be applied
   */
  public CompletableOperationExecutorFactoryWrapper(CompletableComponentExecutorFactory<T> delegate,
                                                    List<Interceptor> interceptors) {
    this.delegate = delegate;
    this.interceptors = interceptors;
  }

  /**
   * @return a {@link CompletableComponentExecutor} which decorates the result of propagating this invocation to the
   * {@link #delegate}
   */
  @Override
  public CompletableComponentExecutor<T> createExecutor(T componentModel, Map<String, Object> parameters) {
    CompletableComponentExecutor<T> executor = delegate.createExecutor(componentModel, parameters);
    return new InterceptableOperationExecutorWrapper(executor, interceptors);
  }

  @Override
  public Function<ExecutionContext<T>, Map<String, Object>> createArgumentResolver(T componentModel) {
    return delegate instanceof OperationArgumentResolverFactory
        ? ((OperationArgumentResolverFactory) delegate).createArgumentResolver(componentModel)
        : ec -> emptyMap();
  }
}
