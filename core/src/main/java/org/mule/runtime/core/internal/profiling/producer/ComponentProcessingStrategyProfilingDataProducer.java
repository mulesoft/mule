/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;

/**
 * Default {@link ProfilingDataProducer} returned by a diagnostic service.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingDataProducer
    implements
    ProfilingDataProducer<org.mule.runtime.core.internal.profiling.context.ComponentProcessingStrategyProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType;

  public ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                          ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
  }

  @Override
  public void triggerProfilingEvent(org.mule.runtime.core.internal.profiling.context.ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
  }
}
