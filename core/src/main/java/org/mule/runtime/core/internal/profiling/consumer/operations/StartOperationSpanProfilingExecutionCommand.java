/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.operations;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanStartProfilingEventContext;

/**
 * A {@link ProfilingExecutionOperation} that triggers a profiling event indicating the start of a span.
 */
public class StartOperationSpanProfilingExecutionCommand
    implements ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext> {

  private final ProfilingDataProducer<SpanStartProfilingEventContext, ComponentProcessingStrategyProfilingEventContext> profilingDataProducer;

  public StartOperationSpanProfilingExecutionCommand(ProfilingService profilingService) {
    profilingDataProducer = profilingService.getProfilingDataProducer(RuntimeProfilingEventTypes.START_SPAN);
  }

  @Override
  public void execute(ComponentProcessingStrategyProfilingEventContext eventContext) {
    profilingDataProducer.triggerProfilingEvent(eventContext, context -> new OperationExecutionStartEventContext(context));

  }

  private class OperationExecutionStartEventContext implements SpanStartProfilingEventContext {

    private final ComponentProcessingStrategyProfilingEventContext eventContext;
    private long triggerTimeStamp;

    public OperationExecutionStartEventContext(ComponentProcessingStrategyProfilingEventContext eventContext) {
      this.eventContext = eventContext;
      triggerTimeStamp = currentTimeMillis();
    }

    @Override
    public long getTriggerTimestamp() {
      return triggerTimeStamp;
    }
  }
}
