/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.AbstractExecutionContextAdapterDecorator;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

class PrecalculatedExecutionContextAdapter extends AbstractExecutionContextAdapterDecorator<OperationModel> {

  private Optional<ConfigurationInstance> configuration;
  private OperationExecutor operation;

  PrecalculatedExecutionContextAdapter(ExecutionContextAdapter<OperationModel> decorated, OperationExecutor operation) {
    super(decorated);

    configuration = decorated.getConfiguration().map(config -> {
      if (config instanceof Interceptable) {
        return new ExecutionContextConfigurationDecorator(config,
                                                          ((Interceptable) config).getInterceptors().stream()
                                                              .map(interceptor -> new InterceptorDecorator(interceptor))
                                                              .collect(toList()));
      } else {
        return config;
      }
    });

    this.operation = new OperationExecutorDecorator(operation);
  }

  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return configuration;
  }

  public OperationExecutor getOperationExecutor() {
    return operation;
  }

  private static class OperationExecutorDecorator implements OperationExecutor, Interceptable {

    private OperationExecutor decorated;
    private List<Interceptor> operationExecutorInterceptors;

    public OperationExecutorDecorator(OperationExecutor decorated) {
      this.decorated = decorated;

      if (decorated instanceof Interceptable) {
        this.operationExecutorInterceptors = ((Interceptable) decorated).getInterceptors().stream()
            .map(interceptor -> new InterceptorDecorator(interceptor))
            .collect(toList());
      } else {
        this.operationExecutorInterceptors = emptyList();
      }
    }

    @Override
    public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
      return decorated.execute(executionContext);
    }

    @Override
    public List<Interceptor> getInterceptors() {
      return operationExecutorInterceptors;
    }

  }

  private static class ExecutionContextConfigurationDecorator implements Interceptable, ConfigurationInstance {

    private ConfigurationInstance decorated;
    private List<Interceptor> interceptors;

    public ExecutionContextConfigurationDecorator(ConfigurationInstance decorated, List<Interceptor> interceptors) {
      this.decorated = decorated;
      this.interceptors = interceptors;
    }

    @Override
    public String getName() {
      return decorated.getName();
    }

    @Override
    public ConfigurationModel getModel() {
      return decorated.getModel();
    }

    @Override
    public Object getValue() {
      return decorated.getValue();
    }

    @Override
    public ConfigurationState getState(Object event) {
      return decorated.getState(event);
    }

    @Override
    public ConfigurationStats getStatistics() {
      return decorated.getStatistics();
    }

    @Override
    public Optional<ConnectionProvider> getConnectionProvider() {
      return decorated.getConnectionProvider();
    }

    @Override
    public List<Interceptor> getInterceptors() {
      return interceptors;
    }

  }

  private static class InterceptorDecorator implements Interceptor {

    private AtomicInteger beforeCalled = new AtomicInteger();
    private Interceptor decorated;

    public InterceptorDecorator(Interceptor decorated) {
      this.decorated = decorated;
    }

    @Override
    public void before(ExecutionContext<OperationModel> executionContext) throws Exception {
      if (beforeCalled.getAndIncrement() == 0) {
        decorated.before(executionContext);
      }
    }

    @Override
    public void onSuccess(ExecutionContext<OperationModel> executionContext, Object result) {
      decorated.onSuccess(executionContext, result);
    }

    @Override
    public Throwable onError(ExecutionContext<OperationModel> executionContext, Throwable exception) {
      return decorated.onError(executionContext, exception);
    }

    @Override
    public void after(ExecutionContext<OperationModel> executionContext, Object result) {
      if (beforeCalled.decrementAndGet() == 0) {
        decorated.after(executionContext, result);
      }
    }
  }
}
