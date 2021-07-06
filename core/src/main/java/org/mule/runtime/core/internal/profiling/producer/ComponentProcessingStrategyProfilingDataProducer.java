/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.internal.profiling.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.profiling.consumer.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;

/**
 * Default {@link ProfilingDataProducer} returned by a diagnostic service.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingDataProducer
    implements ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType;

  public ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                          ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
  }

  @Override
  public void triggerProfilingEvent(ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
  }
}
