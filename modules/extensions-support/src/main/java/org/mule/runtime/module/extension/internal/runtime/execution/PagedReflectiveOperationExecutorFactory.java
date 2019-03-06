/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveExecutorFactoryUtil.createDelegate;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.util.func.CheckedBiFunction;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;

import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Inject;

/**
 * An implementation of {@link ComponentExecutorFactory} which produces instances of {@link PagedReflectiveOperationExecutorFactory}.
 *
 * @param <T> the type of the class in which the implementing method is declared
 * @since 4.1.6, 4.2.0
 */
public final class PagedReflectiveOperationExecutorFactory<T, M extends ComponentModel> implements ComponentExecutorFactory<M> {

  private final Method operationMethod;
  private final PagingProviderResultTransformer pagingProviderTransformer = new PagingProviderResultTransformer();
  private final Class<T> implementationClass;

  public PagedReflectiveOperationExecutorFactory(Class<T> implementationClass, Method operationMethod) {
    this.implementationClass = implementationClass;
    this.operationMethod = operationMethod;
  }

  @Override
  public ComponentExecutor<M> createExecutor(M operationModel, Map<String, Object> parameters) {
    return new ReflectiveMethodOperationExecutor<>(operationModel, operationMethod,
                                                   createDelegate(implementationClass, parameters), pagingProviderTransformer);
  }

  private class PagingProviderResultTransformer implements CheckedBiFunction<Object, ExecutionContext<M>, Object> {

    @Inject
    private ExtensionConnectionSupplier connectionSupplier;

    @Override
    public Object applyChecked(Object result, ExecutionContext<M> executionContext) throws Throwable {
      PagingProviderProducer pagingProviderProducer =
          new PagingProviderProducer<>((PagingProvider) result, executionContext.getConfiguration().get(),
                                       (ExecutionContextAdapter) executionContext, connectionSupplier);
      ListConsumer<?> consumer = new ListConsumer(pagingProviderProducer);
      try {
        consumer.loadNextPage();
      } catch (Throwable e) {
        throw pagingProviderProducer.getCause(e);
      }

      return new ConsumerStreamingIterator<>(consumer);
    }
  }
}
