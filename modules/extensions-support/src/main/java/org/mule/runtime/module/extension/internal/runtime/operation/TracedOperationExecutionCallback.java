/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.tracer.api.EventTracer;

/**
 * A composable {@link ExecutorCallback} that allows to close the operation execution {@link Span} after the operation execution.
 * 
 * @since 4.5
 */
public class TracedOperationExecutionCallback implements ExecutorCallback {

  private final EventedExecutionContext<?> eventedExecutionContext;
  private final EventTracer<CoreEvent> coreEventEventTracer;
  private final ExecutorCallback delegate;

  public TracedOperationExecutionCallback(EventedExecutionContext<?> eventedExecutionContext,
                                          EventTracer<CoreEvent> coreEventEventTracer, ExecutorCallback delegate) {
    this.eventedExecutionContext = eventedExecutionContext;
    this.coreEventEventTracer = coreEventEventTracer;
    this.delegate = delegate;
  }

  @Override
  public void complete(Object value) {
    coreEventEventTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    delegate.complete(value);
  }

  @Override
  public void error(Throwable e) {
    coreEventEventTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    delegate.error(e);
  }
}
