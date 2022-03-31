/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.START_SPAN;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;

/**
 * A {@link ProfilingExecutionOperation} that triggers a profiling event indicating the start of a span.
 *
 * @since 4.5.0
 */
public class StartSpanProfilingExecutionOperation
    implements ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext> {

  private final ProfilingDataProducer<SpanProfilingEventContext, ComponentProcessingStrategyProfilingEventContext> profilingDataProducer;

  public StartSpanProfilingExecutionOperation(ProfilingService profilingService) {
    profilingDataProducer = profilingService.getProfilingDataProducer(START_SPAN);
  }

  @Override
  public void execute(ComponentProcessingStrategyProfilingEventContext eventContext) {
    profilingDataProducer.triggerProfilingEvent(eventContext, context -> new DefaultProfilingEventContext(context));

  }
}
