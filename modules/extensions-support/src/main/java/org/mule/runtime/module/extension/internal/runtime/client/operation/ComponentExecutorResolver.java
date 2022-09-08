/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory.extractExecutorInitialisationParams;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

abstract class ComponentExecutorResolver {

  protected static final Logger LOGGER = getLogger(ComponentExecutorResolver.class);

  protected final OperationKey key;
  private final ExtensionManager extensionManager;
  private final ExpressionManager expressionManager;
  private final ReflectionCache reflectionCache;
  protected final MuleContext muleContext;

  static ComponentExecutorResolver from(OperationKey key,
                                        ExtensionManager extensionManager,
                                        ExpressionManager expressionManager,
                                        ReflectionCache reflectionCache,
                                        MuleContext muleContext) {
    List<ParameterModel> initParameterModels =
        key.getOperationModel().getAllParameterModels().stream()
            .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
            .collect(toList());

    return initParameterModels.isEmpty()
        ? new StaticComponentExecutorResolver(key, extensionManager, expressionManager, reflectionCache, muleContext)
        : new WithInitParamsComponentExecutorResolver(initParameterModels, key, extensionManager,
                                                      expressionManager, reflectionCache, muleContext);
  }

  private ComponentExecutorResolver(OperationKey key,
                                    ExtensionManager extensionManager,
                                    ExpressionManager expressionManager,
                                    ReflectionCache reflectionCache,
                                    MuleContext muleContext) {
    this.key = key;
    this.extensionManager = extensionManager;
    this.expressionManager = expressionManager;
    this.reflectionCache = reflectionCache;
    this.muleContext = muleContext;
  }

  abstract CompletableComponentExecutor<OperationModel> resolveExecutor(Map<String, Object> params);

  protected CompletableComponentExecutor<OperationModel> doCreateExecutor(Map<String, Object> initParams) {
    final OperationModel operationModel = key.getOperationModel();

    CompletableComponentExecutorFactory<OperationModel> operationExecutorFactory = getOperationExecutorFactory(operationModel);
    try {
      initParams.putAll(extractExecutorInitialisationParams(
                                                            key.getExtensionModel(),
                                                            operationModel,
                                                            initParams,
                                                            NULL_COMPONENT,
                                                            empty(),
                                                            extensionManager,
                                                            expressionManager,
                                                            reflectionCache));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(
                                                         "Exception found resolving parameters for operation client: "
                                                             + e.getMessage()),
                                     e);
    }

    CompletableComponentExecutor<OperationModel> executor = operationExecutorFactory.createExecutor(operationModel, initParams);
    try {
      initialiseIfNeeded(executor, true, muleContext);
      startIfNeeded(executor);

      return executor;
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  protected void dispose(CompletableComponentExecutor<OperationModel> executor) {
    try {
      stopIfNeeded(executor);
    } catch (Exception e) {
      LOGGER.error(format("Exception found stopping operation executor. %s", e.getMessage()), e);
    } finally {
      disposeIfNeeded(executor, LOGGER);
    }
  }

  private static class StaticComponentExecutorResolver extends ComponentExecutorResolver implements Disposable {

    private final CompletableComponentExecutor<OperationModel> executor;

    public StaticComponentExecutorResolver(OperationKey key,
                                           ExtensionManager extensionManager,
                                           ExpressionManager expressionManager,
                                           ReflectionCache reflectionCache,
                                           MuleContext muleContext) {
      super(key, extensionManager, expressionManager, reflectionCache, muleContext);
      this.executor = doCreateExecutor(emptyMap());
    }

    @Override
    public CompletableComponentExecutor<OperationModel> resolveExecutor(Map<String, Object> params) {
      return executor;
    }

    @Override
    public void dispose() {
      dispose(executor);
    }
  }

  private static class WithInitParamsComponentExecutorResolver extends ComponentExecutorResolver {

    private List<ParameterModel> initParamModels;

    public WithInitParamsComponentExecutorResolver(List<ParameterModel> initParamModels,
                                                   OperationKey key,
                                                   ExtensionManager extensionManager,
                                                   ExpressionManager expressionManager,
                                                   ReflectionCache reflectionCache,
                                                   MuleContext muleContext) {
      super(key, extensionManager, expressionManager, reflectionCache, muleContext);
      this.initParamModels = initParamModels;
    }

    @Override
    public CompletableComponentExecutor<OperationModel> resolveExecutor(Map<String, Object> params) {
      Map<String, Object> initParams = new HashMap<>();
      initParamModels.forEach(p -> {
        String paramName = p.getName();
        if (params.containsKey(paramName)) {
          initParams.put(paramName, params.get(paramName));
        }
      });

      return new SelfDestructingExecutorDecorator(doCreateExecutor(initParams));
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

  private class SelfDestructingExecutorCallbackDecorator implements CompletableComponentExecutor.ExecutorCallback {

    private final CompletableComponentExecutor<OperationModel> executor;
    private final CompletableComponentExecutor.ExecutorCallback delegate;

    public SelfDestructingExecutorCallbackDecorator(CompletableComponentExecutor<OperationModel> executor,
                                                    CompletableComponentExecutor.ExecutorCallback delegate) {
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
