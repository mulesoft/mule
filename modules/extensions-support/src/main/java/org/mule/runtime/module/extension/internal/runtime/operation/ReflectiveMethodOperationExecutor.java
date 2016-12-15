/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveMethodComponentExecutor;

import java.lang.reflect.Method;

import org.slf4j.Logger;

/**
 * Implementation of {@link OperationExecutor} which works by using reflection to invoke a method from a class.
 *
 * @since 3.7.0
 */
public final class ReflectiveMethodOperationExecutor implements OperationExecutor, MuleContextAware, Lifecycle {

  private static final Logger LOGGER = getLogger(ReflectiveMethodOperationExecutor.class);

  private final ReflectiveMethodComponentExecutor<OperationModel> executor;
  private MuleContext muleContext;

  public ReflectiveMethodOperationExecutor(OperationModel operationModel, Method operationMethod, Object operationInstance) {
    executor =
        new ReflectiveMethodComponentExecutor<>(operationModel.getParameterGroupModels(), operationMethod, operationInstance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object execute(ExecutionContext<OperationModel> executionContext) throws Exception {
    return executor.execute(executionContext);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(executor, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(executor);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(executor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(executor, LOGGER);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
    executor.setMuleContext(muleContext);
  }
}
