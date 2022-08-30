/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

class OperationExecutorSupplier implements Disposable {

  private static final Logger LOGGER = getLogger(OperationExecutorSupplier.class);

  private final Map<OperationKey, ComponentExecutorResolver> cache = new HashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final MuleContext muleContext;

  OperationExecutorSupplier(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  CompletableComponentExecutor<OperationModel> getComponentExecutor(OperationKey key, Map<String, Object> params) {
    ComponentExecutorResolver creator;
    readLock.lock();
    try {
      creator = cache.get(key);
      if (creator != null) {
        return creator.createExecutor(params);
      }

      readLock.unlock();
      writeLock.lock();

      try {
        creator = cache.get(key);
        if (creator != null) {
          return creator.createExecutor(params);
        }

        creator = getExecutorResolver(key.getOperationModel());
        if (creator.isCacheable()) {
          cache.put(key, creator);
        }

        readLock.lock();
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }

    return creator.createExecutor(params);
  }

  private ComponentExecutorResolver getExecutorResolver(OperationModel operationModel) {
    List<ParameterModel> initParameterModels =
        operationModel.getAllParameterModels().stream()
            .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
            .collect(toList());

    return initParameterModels.isEmpty()
        ? new StaticComponentExecutorResolver(operationModel, muleContext)
        : new WithInitParamsComponentExecutorResolver(initParameterModels, operationModel, muleContext);
  }

  @Override
  public void dispose() {
    writeLock.lock();
    try {
      cache.values().forEach(ComponentExecutorResolver::dispose);
    } finally {
      writeLock.unlock();
    }
  }

  private void dispose(CompletableComponentExecutor<OperationModel> executor) {
    try {
      stopIfNeeded(executor);
    }catch (Exception e) {
      LOGGER.error(format("Exception found stopping operation executor. %s", e.getMessage()), e);
    } finally {
      disposeIfNeeded(executor, LOGGER);
    }
  }

  private static abstract class ComponentExecutorResolver implements Disposable {

    protected final OperationModel operationModel;
    protected final MuleContext muleContext;

    public ComponentExecutorResolver(OperationModel operationModel, MuleContext muleContext) {
      this.operationModel = operationModel;
      this.muleContext = muleContext;
    }

    abstract CompletableComponentExecutor<OperationModel> createExecutor(Map<String, Object> params);

    abstract boolean isCacheable();

    protected CompletableComponentExecutor<OperationModel> doCreateExecutor(Map<String, Object> initParams) {
      CompletableComponentExecutor<OperationModel> executor =
          getOperationExecutorFactory(operationModel).createExecutor(operationModel, initParams);
      try {
        initialiseIfNeeded(executor, true, muleContext);
        startIfNeeded(executor);

        return executor;
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private class StaticComponentExecutorResolver extends ComponentExecutorResolver {

    private final CompletableComponentExecutor<OperationModel> executor;

    public StaticComponentExecutorResolver(OperationModel operationModel, MuleContext muleContext) {
      super(operationModel, muleContext);
      this.executor = doCreateExecutor(emptyMap());
    }

    @Override
    public CompletableComponentExecutor<OperationModel> createExecutor(Map<String, Object> params) {
      return executor;
    }

    @Override
    boolean isCacheable() {
      return true;
    }

    @Override
    public void dispose() {
      OperationExecutorSupplier.this.dispose(executor);
    }
  }

  private class WithInitParamsComponentExecutorResolver extends ComponentExecutorResolver {

    private List<ParameterModel> initParamModels;

    public WithInitParamsComponentExecutorResolver(List<ParameterModel> initParamModels,
                                                   OperationModel operationModel,
                                                   MuleContext muleContext) {
      super(operationModel, muleContext);
      this.initParamModels = initParamModels;
    }

    @Override
    public CompletableComponentExecutor<OperationModel> createExecutor(Map<String, Object> params) {
      Map<String, Object> initParams = new HashMap<>();
      initParamModels.forEach(p -> {
        String paramName = p.getName();
        if (params.containsKey(paramName)) {
          initParams.put(paramName, params.get(paramName));
        }
      });

      return new SelfDestructingExecutorDecorator(doCreateExecutor(initParams));
    }

    @Override
    boolean isCacheable() {
      return false;
    }

    @Override
    public void dispose() {
      initParamModels.clear();
    }
  }

  private class SelfDestructingExecutorDecorator implements CompletableComponentExecutor<OperationModel> {

    private final CompletableComponentExecutor<OperationModel> delegate;

    private SelfDestructingExecutorDecorator(CompletableComponentExecutor<OperationModel> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void execute(ExecutionContext<OperationModel> executionContext, ExecutorCallback callback) {
      delegate.execute(executionContext, new SelfDestructingExecutorCallbackDecorator(delegate, callback));
    }
  }

  private class SelfDestructingExecutorCallbackDecorator implements ExecutorCallback {

    private final CompletableComponentExecutor<OperationModel> executor;
    private final ExecutorCallback delegate;

    public SelfDestructingExecutorCallbackDecorator(CompletableComponentExecutor<OperationModel> executor, ExecutorCallback delegate) {
      this.executor = executor;
      this.delegate = delegate;
    }

    @Override
    public void complete(Object value) {
      try {
        delegate.complete(value);
      } finally {
        dispose(executor);
      }
    }

    @Override
    public void error(Throwable e) {
      try {
        delegate.error(e);
      } finally {
        dispose(executor);
      }
    }
  }
}
