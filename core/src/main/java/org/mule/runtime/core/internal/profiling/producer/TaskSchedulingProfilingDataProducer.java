/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TaskSchedulingProfilingEventContext;
import org.mule.runtime.api.profiling.type.TaskSchedulingProfilingEventType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingDataProducerStatus;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

import java.util.function.Function;

/**
 * A {@link ProfilingDataProducer} for producing data related to task scheduling.
 *
 * @see TaskSchedulingProfilingEventType
 * @see Scheduler
 * @since 4.5
 */
public class TaskSchedulingProfilingDataProducer
    implements ResettableProfilingDataProducer<TaskSchedulingProfilingEventContext, CoreEvent> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType;
  private final ProfilingDataProducerStatus profilingProducerStatus;

  public TaskSchedulingProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                             ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType,
                                             ProfilingProducerScope profilingProducerScope,
                                             ProfilingFeatureFlaggingService featureFlaggingService) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.profilingProducerStatus =
        featureFlaggingService.getProfilingDataProducerStatus(profilingEventType, profilingProducerScope);
  }

  @Override
  public void triggerProfilingEvent(TaskSchedulingProfilingEventContext profilerEventContext) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(profilerEventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(CoreEvent sourceData,
                                    Function<CoreEvent, TaskSchedulingProfilingEventContext> transformation) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(transformation.apply(sourceData), profilingEventType);
    }
  }

  @Override
  public void reset() {
    profilingProducerStatus.reset();
  }
}
