/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Collections.emptyMap;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Decorates a {@link ComponentExecutorFactory} so that the instances that it generates are also decorated through a
 * {@link InterceptableOperationExecutorWrapper}, so that the items in the {@link #interceptors} list can apply.
 *
 * @since 4.0
 */
public final class OperationExecutorFactoryWrapper<T extends ComponentModel>
    implements ComponentExecutorFactory<T>, OperationArgumentResolverFactory<T> {

  private final ComponentExecutorFactory delegate;
  private final List<Interceptor> interceptors;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ComponentExecutorFactory} to be decorated
   * @param interceptors a {@link List} with the {@link Interceptor interceptors} that should aply
   */
  public OperationExecutorFactoryWrapper(ComponentExecutorFactory<T> delegate, List<Interceptor> interceptors) {
    this.delegate = delegate;
    this.interceptors = interceptors;
  }

  /**
   * @return a {@link InterceptableOperationExecutorWrapper} which decorates the result of propagating this invocation to the
   *         {@link #delegate}
   */

  @Override
  public ComponentExecutor<T> createExecutor(T operationModel, Map<String, Object> parameters) {
    ComponentExecutor executor = delegate.createExecutor(operationModel, parameters);
    executor = new ReactiveOperationExecutionWrapper(executor);
    executor = new InterceptableOperationExecutorWrapper(executor, interceptors);

    return executor;
  }

  @Override
  public Function<ExecutionContext<T>, Map<String, Object>> createArgumentResolver(T operationModel) {
    return delegate instanceof OperationArgumentResolverFactory
        ? ((OperationArgumentResolverFactory) delegate).createArgumentResolver(operationModel)
        : ec -> emptyMap();
  }
}
