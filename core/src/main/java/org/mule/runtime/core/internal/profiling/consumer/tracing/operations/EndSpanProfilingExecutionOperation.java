/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.END_SPAN;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

/**
 * A {@link ProfilingExecutionOperation} that triggers a profiling event indicating the end of a span.
 *
 * @since 4.5.0
 */
public class EndSpanProfilingExecutionOperation extends SpanProfilingExecutionOperation {

  public EndSpanProfilingExecutionOperation(InternalProfilingService profilingService) {
    super(profilingService);
  }

  @Override
  protected ProfilingEventType<SpanProfilingEventContext> getProfilingEventType() {
    return END_SPAN;
  }
}
