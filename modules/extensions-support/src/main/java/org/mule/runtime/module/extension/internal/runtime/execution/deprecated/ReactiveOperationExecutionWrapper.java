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
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

/**
 * Decorates an {@link ComponentExecutor} adding the necessary logic to execute non blocking operations.
 * <p>
 * If the operation being executed is blocking, then it delegates to the wrapped executor transparently. If the operation is non
 * blocking, then it creates and injects a {@link CompletionCallback} on which the operation result will be notified.
 * <p>
 * It also implements {@link Lifecycle} and {@link MuleContextAware}, propagating those to the decoratee if necessary
 *
 * @since 4.0
 */
public final class ReactiveOperationExecutionWrapper<M extends ComponentModel>
    implements ComponentExecutor<M>, OperationArgumentResolverFactory<M>, Lifecycle, MuleContextAware {

  private static final Logger LOGGER = getLogger(ReactiveOperationExecutionWrapper.class);

  private final ComponentExecutor<M> delegate;
  private MuleContext muleContext;

  public ReactiveOperationExecutionWrapper(ComponentExecutor delegate) {
    this.delegate = delegate;
  }

  @Override
  public Publisher<Object> execute(ExecutionContext<M> executionContext) {
    ExecutionContextAdapter<M> context = (ExecutionContextAdapter<M>) executionContext;
    return Mono.create(sink -> {
      MonoCompletionCallback callback = new MonoCompletionCallback(sink);
      context.setVariable(COMPLETION_CALLBACK_CONTEXT_PARAM, callback);

      try {
        delegate.execute(executionContext);
      } catch (Throwable t) {
        sink.error(wrapFatal(t));
      }
    });
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

  @Override
  public Function<ExecutionContext<M>, Map<String, Object>> createArgumentResolver(M operationModel) {
    return delegate instanceof OperationArgumentResolverFactory
        ? ((OperationArgumentResolverFactory) delegate).createArgumentResolver(operationModel)
        : ec -> emptyMap();
  }
}
