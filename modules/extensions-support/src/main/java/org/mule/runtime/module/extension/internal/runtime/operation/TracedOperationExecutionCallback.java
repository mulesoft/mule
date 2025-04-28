/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.tracer.api.component.ComponentTracer;

/**
 * A composable {@link ExecutorCallback} that allows to close the operation execution {@link Span} after the operation execution.
 *
 * @since 4.5
 */
public class TracedOperationExecutionCallback implements ExecutorCallback {

  private final EventedExecutionContext<?> eventedExecutionContext;
  private final ComponentTracer<CoreEvent> operationExecutionTracer;
  private final ExecutorCallback delegate;

  public TracedOperationExecutionCallback(EventedExecutionContext<?> eventedExecutionContext,
                                          ComponentTracer<CoreEvent> operationExecutionTracer, ExecutorCallback delegate) {
    this.eventedExecutionContext = eventedExecutionContext;
    this.operationExecutionTracer = operationExecutionTracer;
    this.delegate = delegate;
  }

  @Override
  public void complete(Object value) {
    operationExecutionTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    delegate.complete(value);
  }

  @Override
  public void error(Throwable e) {
    operationExecutionTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    delegate.error(e);
  }
}
