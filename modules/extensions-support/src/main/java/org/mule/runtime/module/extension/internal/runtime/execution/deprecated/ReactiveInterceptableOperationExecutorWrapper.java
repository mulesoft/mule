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
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.module.extension.internal.loader.AbstractInterceptable;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * Decorates an {@link ComponentExecutor} adding the behavior defined in {@link AbstractInterceptable}.
 * <p>
 * Dependency injection and lifecycle phases will also be propagated to the {@link #delegate}
 *
 * @since 4.0
 * @deprecated since 4.2
 */
@Deprecated
public final class ReactiveInterceptableOperationExecutorWrapper<M extends ComponentModel> extends AbstractInterceptable
    implements ComponentExecutor<M>, OperationArgumentResolverFactory<M> {

  private static final Logger LOGGER = getLogger(ReactiveInterceptableOperationExecutorWrapper.class);

  private final ComponentExecutor delegate;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ComponentExecutor} to be decorated
   * @param interceptors the {@link Interceptor interceptors} that should apply to the {@code delegate}
   */
  public ReactiveInterceptableOperationExecutorWrapper(ComponentExecutor<M> delegate, List<Interceptor> interceptors) {
    super(interceptors);
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

  @Override
  public Function<ExecutionContext<M>, Map<String, Object>> createArgumentResolver(M operationModel) {
    return delegate instanceof OperationArgumentResolverFactory
        ? ((OperationArgumentResolverFactory) delegate).createArgumentResolver(operationModel)
        : ec -> emptyMap();
  }
}
