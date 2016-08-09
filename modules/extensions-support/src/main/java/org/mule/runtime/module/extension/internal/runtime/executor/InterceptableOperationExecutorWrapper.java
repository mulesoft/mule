/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.introspection.AbstractInterceptable;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates an {@link OperationExecutor} adding the behavior defined in {@link AbstractInterceptable}.
 * <p>
 * Dependency injection and lifecycle phases will also be propagated to the {@link #delegate}
 *
 * @since 4.0
 */
public final class InterceptableOperationExecutorWrapper extends AbstractInterceptable implements OperationExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(InterceptableOperationExecutorWrapper.class);

  private final OperationExecutor delegate;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link OperationExecutor} to be decorated
   * @param interceptors the {@link Interceptor interceptors} that should apply to the {@code delegate}
   */
  public InterceptableOperationExecutorWrapper(OperationExecutor delegate, List<Interceptor> interceptors) {
    super(interceptors);
    this.delegate = delegate;
  }

  /**
   * Directly delegates into {@link #delegate} {@inheritDoc}
   */
  @Override
  public <T> T execute(OperationContext operationContext) throws Exception {
    return delegate.execute(operationContext);
  }

  /**
   * Performs dependency injection into the {@link #delegate} and the items in the {@link #interceptors} list.
   * <p>
   * Then it propagates this lifecycle phase into them.
   *
   * @throws InitialisationException in case of error
   */
  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
    super.initialise();
  }

  /**
   * Propagates this lifecycle phase into the items in the {@link #interceptors} list and the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void start() throws MuleException {
    super.start();
    startIfNeeded(delegate);
  }

  /**
   * Propagates this lifecycle phase into the items in the {@link #interceptors} list and the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void stop() throws MuleException {
    super.stop();
    stopIfNeeded(delegate);
  }

  /**
   * Propagates this lifecycle phase into the items in the {@link #interceptors} list and the {@link #delegate}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void dispose() {
    super.dispose();
    disposeIfNeeded(delegate, LOGGER);
  }
}
