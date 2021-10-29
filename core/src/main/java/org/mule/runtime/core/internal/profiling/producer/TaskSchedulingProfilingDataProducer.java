/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TaskSchedulingProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;

public class TaskSchedulingProfilingDataProducer implements ProfilingDataProducer<TaskSchedulingProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType;

  public TaskSchedulingProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                             ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
  }

  @Override
  public void triggerProfilingEvent(TaskSchedulingProfilingEventContext profilerEventContext) {
    defaultProfilingService.notifyEvent(profilerEventContext, profilingEventType);
  }
}
