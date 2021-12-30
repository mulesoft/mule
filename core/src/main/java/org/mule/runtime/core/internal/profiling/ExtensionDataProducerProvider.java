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
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.internal.profiling.producer.ExtensionProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

/**
 * A {@link ProfilingDataProducerProvider} that provides {@link ExtensionProfilingDataProducer}
 *
 * @since 4.5.0
 */
public class ExtensionDataProducerProvider
    implements ProfilingDataProducerProvider<ExtensionProfilingEventContext, Object> {

  private final DefaultProfilingService profilingService;
  private final ProfilingEventType<ExtensionProfilingEventContext> profilingEventType;
  private final ProfilingFeatureFlaggingService featureFlaggingService;

  public ExtensionDataProducerProvider(DefaultProfilingService profilingService,
                                       ProfilingEventType<ExtensionProfilingEventContext> profilingEventType,
                                       ProfilingFeatureFlaggingService featureFlaggingService) {
    this.profilingService = profilingService;
    this.profilingEventType = profilingEventType;
    this.featureFlaggingService = featureFlaggingService;

  }

  @Override
  public <T extends ProfilingEventContext, S> ResettableProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                             ProfilingProducerScope profilingProducerScope) {
    return (ResettableProfilingDataProducer<T, S>) new ExtensionProfilingDataProducer(profilingService, profilingEventType,
                                                                                      profilingProducerScope,
                                                                                      featureFlaggingService);
  }
}
