/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingDataProducerStatus;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

import java.util.function.Function;

public class StartSpanProfilingDataProducer implements
    ResettableProfilingDataProducer<SpanProfilingEventContext, ComponentProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<SpanProfilingEventContext> profilingEventType;
  private final ProfilingDataProducerStatus profilingProducerStatus;

  public StartSpanProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                        ProfilingEventType<SpanProfilingEventContext> profilingEventType,
                                        ProfilingProducerScope profilingProducerScope,
                                        ProfilingFeatureFlaggingService featureFlaggingService) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.profilingProducerStatus =
        featureFlaggingService.getProfilingDataProducerStatus(profilingEventType, profilingProducerScope);
  }

  @Override
  public void triggerProfilingEvent(SpanProfilingEventContext profilerEventContext) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(profilerEventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(ComponentProfilingEventContext componentProfilingEventContext,
                                    Function<ComponentProfilingEventContext, SpanProfilingEventContext> transformation) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(transformation.apply(componentProfilingEventContext), profilingEventType);
    }
  }

  @Override
  public void reset() {
    profilingProducerStatus.reset();
  }
}
