/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.producer;

import org.mule.runtime.core.api.diagnostics.DefaultDiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
import org.mule.runtime.core.api.diagnostics.ProfilingEventType;
import org.mule.runtime.core.api.diagnostics.consumer.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;

/**
 * Default {@link ProfilingDataProducer} returned by a diagnostic service.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingDataProducer
    implements ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext> {

  private final DefaultDiagnosticsService diagnosticService;
  private final ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType;

  public ComponentProcessingStrategyProfilingDataProducer(DefaultDiagnosticsService diagnosticService,
                                                          ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType) {
    this.diagnosticService = diagnosticService;
    this.profilingEventType = profilingEventType;
  }

  @Override
  public void event(ComponentProcessingStrategyProfilingEventContext profilerEventContext) {
    diagnosticService.notifyEvent(profilerEventContext, profilingEventType);
  }
}
