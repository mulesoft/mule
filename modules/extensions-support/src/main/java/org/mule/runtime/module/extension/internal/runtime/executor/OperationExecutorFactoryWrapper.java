/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;

import java.util.List;

/**
 * Decorates a {@link OperationExecutorFactory} so that the instances that it generates are also decorated through a
 * {@link InterceptableOperationExecutorWrapper}, so that the items in the {@link #interceptors} list can apply.
 *
 * @since 4.0
 */
public final class OperationExecutorFactoryWrapper implements OperationExecutorFactory {

  private final OperationExecutorFactory delegate;
  private final List<Interceptor> interceptors;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link OperationExecutorFactory} to be decorated
   * @param interceptors a {@link List} with the {@link Interceptor interceptors} that should aply
   */
  public OperationExecutorFactoryWrapper(OperationExecutorFactory delegate, List<Interceptor> interceptors) {
    this.delegate = delegate;
    this.interceptors = interceptors;
  }

  /**
   * @return a {@link InterceptableOperationExecutorWrapper} which decorates the result of propagating this invocation to the
   *         {@link #delegate}
   */
  @Override
  public OperationExecutor createExecutor(OperationModel operationModel) {
    OperationExecutor executor = delegate.createExecutor(operationModel);
    return new InterceptableOperationExecutorWrapper(executor, interceptors);
  }
}
