/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.deprecated;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Map;

import org.slf4j.Logger;

public class ComponentExecutorCompletableAdapterFactory<T extends ComponentModel>
    implements CompletableComponentExecutorFactory<T> {

  private static final Logger LOGGER = getLogger(ComponentExecutorCompletableAdapterFactory.class);

  private final ComponentExecutorFactory<T> delegate;

  public ComponentExecutorCompletableAdapterFactory(ComponentExecutorFactory<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableComponentExecutor<T> createExecutor(T componentModel, Map<String, Object> parameters) {
    return new ComponentExecutorCompletableAdapter(delegate.createExecutor(componentModel, parameters));
  }

  private static class ComponentExecutorCompletableAdapter<T extends ComponentModel> implements CompletableComponentExecutor<T>,
      Lifecycle, MuleContextAware {

    private final ComponentExecutor<T> delegate;
    private MuleContext muleContext;

    public ComponentExecutorCompletableAdapter(ComponentExecutor<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void execute(ExecutionContext<T> executionContext, ExecutorCallback callback) {
      from(delegate.execute(executionContext)).subscribe(callback::complete, callback::error);
    }

    @Override
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate, true, muleContext);
    }

    @Override
    public void start() throws MuleException {
      startIfNeeded(delegate);
    }

    @Override
    public void stop() throws MuleException {
      stopIfNeeded(delegate);
    }

    @Override
    public void dispose() {
      disposeIfNeeded(delegate, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext muleContext) {
      this.muleContext = muleContext;
    }
  }
}
