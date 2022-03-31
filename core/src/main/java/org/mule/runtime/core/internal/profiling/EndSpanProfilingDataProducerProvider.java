/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.producer.EndSpanProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

/**
 * A {@link ProfilingDataProducerProvider} that provides {@link EndSpanProfilingDataProducer}
 */
public class EndSpanProfilingDataProducerProvider
    implements ProfilingDataProducerProvider<SpanProfilingEventContext, EndSpanProfilingDataProducer> {

  private final DefaultProfilingService profilingService;
  private final ProfilingEventType<SpanProfilingEventContext> profilingEventType;
  private final ProfilingFeatureFlaggingService featureFlaggingService;

  public EndSpanProfilingDataProducerProvider(DefaultProfilingService profilingService,
                                              ProfilingEventType<SpanProfilingEventContext> profilingEventType,
                                              ProfilingFeatureFlaggingService featureFlaggingService) {
    this.profilingService = profilingService;
    this.profilingEventType = profilingEventType;
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public <T extends ProfilingEventContext, S> ResettableProfilingDataProducer<T, S> getProfilingDataProducer(ProfilingProducerScope profilingProducerScope) {
    return (ResettableProfilingDataProducer<T, S>) new EndSpanProfilingDataProducer(profilingService, profilingEventType,
                                                                                    profilingProducerScope,
                                                                                    featureFlaggingService);
  }
}
