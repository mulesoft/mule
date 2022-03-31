/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.END_SPAN;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.ComponentSpanBuilder;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanBuilder;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.SpanBuilder;;

/**
 * A {@link ProfilingExecutionOperation} that triggers a profiling event indicating the end of a span.
 *
 * @since 4.5.0
 */
public class EndSpanProfilingExecutionOperation implements
    ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext> {

  private final ProfilingDataProducer<SpanProfilingEventContext, ComponentProcessingStrategyProfilingEventContext> profilingDataProducer;

  public EndSpanProfilingExecutionOperation(ProfilingService profilingService) {
    profilingDataProducer = profilingService.getProfilingDataProducer(END_SPAN);
  }

  @Override
  public void execute(ComponentProcessingStrategyProfilingEventContext eventContext) {
    profilingDataProducer.triggerProfilingEvent(eventContext, context -> new OperationExecutionEndEventContext(context));
  }

  /**
   * A {@link SpanProfilingEventContext} that corresponds to an end span profiling event.
   */
  private class OperationExecutionEndEventContext implements SpanProfilingEventContext {

    private final ComponentProcessingStrategyProfilingEventContext eventContext;
    private long triggerTimeStamp;

    public OperationExecutionEndEventContext(ComponentProcessingStrategyProfilingEventContext eventContext) {
      this.eventContext = eventContext;
      triggerTimeStamp = currentTimeMillis();
    }

    @Override
    public long getTriggerTimestamp() {
      return triggerTimeStamp;
    }

    @Override
    public Span getSpan() {
      return getBuilder(eventContext.getLocation().get())
          .withArtifactId(eventContext.getArtifactId())
          .withLocation(eventContext.getLocation().get())
          .withCorrelationId(eventContext.getCorrelationId())
          .build();
    }

    private SpanBuilder getBuilder(ComponentLocation location) {
      if (location.getComponentIdentifier()
          .getType().equals(TypedComponentIdentifier.ComponentType.FLOW)) {
        return ComponentSpanBuilder.builder();
      } else {
        return FlowSpanBuilder.builder();
      }
    }
  }
}
