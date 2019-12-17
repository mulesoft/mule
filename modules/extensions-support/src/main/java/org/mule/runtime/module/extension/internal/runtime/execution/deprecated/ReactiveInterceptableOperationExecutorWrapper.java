/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.deprecated;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.AbstractInterceptable;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;

import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * Decorates an {@link ComponentExecutor} adding the behavior defined in {@link AbstractInterceptable}.
 * <p>
 * Dependency injection and lifecycle phases will also be propagated to the {@link #delegate}
 *
 * @since 4.0
 * @deprecated since 4.3
 */
@Deprecated
public final class ReactiveInterceptableOperationExecutorWrapper<M extends ComponentModel>
    implements ComponentExecutor<M>, OperationArgumentResolverFactory<M>, Lifecycle {

  private static final Logger LOGGER = getLogger(ReactiveInterceptableOperationExecutorWrapper.class);

  @Inject
  protected MuleContext muleContext;

  private final ComponentExecutor delegate;

  /**
   * Creates a new instance
   *
   * @param delegate     the {@link ComponentExecutor} to be decorated
   */
  public ReactiveInterceptableOperationExecutorWrapper(ComponentExecutor<M> delegate) {
    this.delegate = delegate;
  }

  /**
   * Directly delegates into {@link #delegate} {@inheritDoc}
   */
  @Override
  public Publisher<Object> execute(ExecutionContext<M> executionContext) {
    return delegate.execute(executionContext);
  }

  /**
   * Performs dependency injection into the {@link #delegate}.
   * <p>
   * Then it propagates this lifecycle phase into them.
   *
   * @throws InitialisationException in case of error
   */
  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
  }

  /**
   * Propagates this lifecycle phase into the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void start() throws MuleException {
    startIfNeeded(delegate);
  }

  /**
   * Propagates this lifecycle phase into the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void stop() throws MuleException {
    stopIfNeeded(delegate);
  }

  /**
   * Propagates this lifecycle phase into the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }

  @Override
  public Function<ExecutionContext<M>, Map<String, Object>> createArgumentResolver(M operationModel) {
    return delegate instanceof OperationArgumentResolverFactory
        ? ((OperationArgumentResolverFactory) delegate).createArgumentResolver(operationModel)
        : ec -> emptyMap();
  }
}
