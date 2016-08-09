/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OperationExecutor} which relies on a {@link #executorDelegate} and a reference to one of its
 * {@link Method}s. When {@link #execute(OperationContext)} is invoked, the {@link #operationMethod} is invoked over the
 * {@link #executorDelegate}.
 * <p/>
 * All the {@link Lifecycle} events that {@code this} instance receives are propagated to the {@link #executorDelegate}
 *
 * @since 3.7.0
 */
public final class ReflectiveMethodOperationExecutor implements OperationExecutor, MuleContextAware, Lifecycle {

  private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate {

    private static final Object[] EMPTY = new Object[] {};

    @Override
    public Object[] resolve(OperationContext operationContext, Class<?>[] parameterTypes) {
      return EMPTY;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ReflectiveMethodOperationExecutor.class);
  private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

  private final Method operationMethod;
  private final Object executorDelegate;
  private final ArgumentResolverDelegate argumentResolverDelegate;
  private final ClassLoader extensionClassLoader;

  private MuleContext muleContext;

  ReflectiveMethodOperationExecutor(OperationModel operationModel, Method operationMethod, Object executorDelegate) {
    this.operationMethod = operationMethod;
    this.executorDelegate = executorDelegate;
    argumentResolverDelegate = isEmpty(operationMethod.getParameterTypes()) ? NO_ARGS_DELEGATE
        : new MethodArgumentResolverDelegate(operationModel, operationMethod);
    extensionClassLoader = operationMethod.getDeclaringClass().getClassLoader();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object execute(OperationContext operationContext) throws Exception {
    return withContextClassLoader(extensionClassLoader,
                                  () -> invokeMethod(operationMethod, executorDelegate,
                                                     getParameterValues(operationContext, operationMethod.getParameterTypes())));
  }

  private Object[] getParameterValues(OperationContext operationContext, Class<?>[] parameterTypes) {
    return argumentResolverDelegate.resolve(operationContext, parameterTypes);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(executorDelegate, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(executorDelegate);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(executorDelegate);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(executorDelegate, LOGGER);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
    if (executorDelegate instanceof MuleContextAware) {
      ((MuleContextAware) executorDelegate).setMuleContext(context);
    }
  }
}
