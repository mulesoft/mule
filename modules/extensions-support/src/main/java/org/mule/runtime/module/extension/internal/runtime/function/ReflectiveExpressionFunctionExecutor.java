/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.function;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * Executes a task associated to a {@link ExecutionContext} by invoking a given {@link Method}
 *
 * @since 4.0
 */
public class ReflectiveExpressionFunctionExecutor implements Lifecycle, FunctionExecutor {

  private static final Logger LOGGER = getLogger(ReflectiveExpressionFunctionExecutor.class);

  private final Method method;
  private final FunctionModel model;
  private final DataType returnType;
  private final Object componentInstance;
  private final ClassLoader extensionClassLoader;
  private final List<FunctionParameter> functionParameters;

  public ReflectiveExpressionFunctionExecutor(FunctionModel model, DataType returnType,
                                              List<FunctionParameter> functionParameters, Method method,
                                              Object componentInstance) {
    this.model = model;
    this.method = method;
    this.returnType = returnType;
    this.componentInstance = componentInstance;
    this.functionParameters = functionParameters;
    this.extensionClassLoader = method.getDeclaringClass().getClassLoader();
  }

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    return withContextClassLoader(extensionClassLoader, () -> invokeMethod(method, componentInstance, parameters));
  }

  @Override
  public Optional<DataType> returnType() {
    return ofNullable(returnType);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return functionParameters;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(componentInstance);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(componentInstance);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(componentInstance);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(componentInstance, LOGGER);
  }

}
