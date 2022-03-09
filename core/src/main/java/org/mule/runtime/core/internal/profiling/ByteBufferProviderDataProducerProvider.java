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
import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;
import org.mule.runtime.core.internal.profiling.producer.ByteBufferProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

import java.util.Map;

/**
 * A {@link ProfilingDataProducerProvider} that provides {@link ByteBufferProfilingDataProducer}
 *
 * @since 4.5.0
 */
public class ByteBufferProviderDataProducerProvider
    implements ProfilingDataProducerProvider<ByteBufferProviderEventContext, Map> {

  private final DefaultProfilingService profilingService;
  private final ProfilingFeatureFlaggingService featureFlaggingService;
  private final ProfilingEventType<ByteBufferProviderEventContext> profilingEventType;

  public ByteBufferProviderDataProducerProvider(DefaultProfilingService profilingService,
                                                ProfilingEventType<ByteBufferProviderEventContext> profiliingEventType,
                                                ProfilingFeatureFlaggingService featureFlaggingService) {
    this.profilingService = profilingService;
    this.profilingEventType = profiliingEventType;
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public <T extends ProfilingEventContext, S> ResettableProfilingDataProducer<T, S> getProfilingDataProducer(ProfilingProducerScope profilingProducerScope) {
    return (ResettableProfilingDataProducer<T, S>) new ByteBufferProfilingDataProducer(profilingService, profilingEventType,
                                                                                       profilingProducerScope,
                                                                                       featureFlaggingService);
  }
}
