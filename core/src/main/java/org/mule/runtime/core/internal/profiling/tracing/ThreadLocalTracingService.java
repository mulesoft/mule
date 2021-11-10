/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import org.mule.runtime.api.profiling.tracing.ExecutionContext;
import org.mule.runtime.api.profiling.tracing.TracingService;

import java.util.Objects;

/**
 * {@link TracingService} implementation that stores the tracing data as {@link ThreadLocal} values.
 */
public class ThreadLocalTracingService implements TracingService {

  private static final ThreadLocal<ExecutionContext> currentExecutionContext = new ThreadLocal<>();

  public ThreadLocalTracingService() {}

  @Override
  public ExecutionContext getCurrentExecutionContext() {
    return currentExecutionContext.get();
  }

  @Override
  public void deleteCurrentExecutionContext() {
    currentExecutionContext.remove();
  }

  @Override
  public ExecutionContext setCurrentExecutionContext(ExecutionContext tracingContext) {
    // Since TracingContext is immutable, it can be set as is (it would require creating a copy otherwise).
    Objects.requireNonNull(tracingContext);
    currentExecutionContext.set(tracingContext);
    return tracingContext;
  }
}
