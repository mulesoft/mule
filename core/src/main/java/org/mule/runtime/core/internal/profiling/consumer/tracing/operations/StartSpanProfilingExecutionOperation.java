/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.START_SPAN;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.SpanManager;

/**
 * A {@link ProfilingExecutionOperation} that triggers a profiling event indicating the start of a span.
 *
 * @since 4.5.0
 */
public class StartSpanProfilingExecutionOperation
    extends SpanProfilingExecutionOperation {

  public StartSpanProfilingExecutionOperation(InternalProfilingService profilingService) {
    super(profilingService);
  }

  @Override
  protected ProfilingEventType<SpanProfilingEventContext> getProfilingEventType() {
    return START_SPAN;
  }

  @Override
  protected SpanProfilingEventContext getSpanEventContext(ComponentProcessingStrategyProfilingEventContext processingStrategyEventContext,
                                                          SpanManager spanManager) {
    return new DefaultSpanProfilingEventContext(processingStrategyEventContext, spanManager);
  }
}
