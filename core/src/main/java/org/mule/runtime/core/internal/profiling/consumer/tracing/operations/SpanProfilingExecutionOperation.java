/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.SpanManager;

public abstract class SpanProfilingExecutionOperation implements
    ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext> {

  private final ProfilingDataProducer<SpanProfilingEventContext, ComponentProcessingStrategyProfilingEventContext> profilingDataProducer;
  private final SpanManager spanManager;

  public SpanProfilingExecutionOperation(InternalProfilingService profilingService) {
    profilingDataProducer = profilingService.getProfilingDataProducer(getProfilingEventType());
    spanManager = profilingService.getSpanManager();
  }

  @Override
  public void execute(ComponentProcessingStrategyProfilingEventContext processingStrategyEventContext) {
    profilingDataProducer.triggerProfilingEvent(processingStrategyEventContext,
                                                sourceContext -> getSpanEventContext(sourceContext, spanManager));
  }

  protected abstract ProfilingEventType<SpanProfilingEventContext> getProfilingEventType();

  protected abstract SpanProfilingEventContext getSpanEventContext(ComponentProcessingStrategyProfilingEventContext processingStrategyEventContext,
                                                                   SpanManager spanManager);

}
