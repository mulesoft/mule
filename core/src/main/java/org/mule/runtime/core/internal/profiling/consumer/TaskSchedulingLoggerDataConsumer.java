/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.SCHEDULING_TASK_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_TASK_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TASK_EXECUTED;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getTaskSchedulingInfoMap;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TaskSchedulingProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;

import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import com.google.gson.Gson;

/**
 * A {@link ProfilingDataConsumer} that logs task scheduling profiling events.
 */
@RuntimeInternalProfilingDataConsumer
public class TaskSchedulingLoggerDataConsumer implements ProfilingDataConsumer<TaskSchedulingProfilingEventContext> {

  private static final Logger LOGGER = getLogger(TaskSchedulingLoggerDataConsumer.class);

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType,
                               TaskSchedulingProfilingEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getTaskSchedulingInfoMap(profilingEventType, profilingEventContext)));
    }
  }

  @Override
  public Set<ProfilingEventType<TaskSchedulingProfilingEventContext>> getProfilingEventTypes() {
    return of(SCHEDULING_TASK_EXECUTION, STARTING_TASK_EXECUTION, TASK_EXECUTED);
  }

  @Override
  public Predicate<TaskSchedulingProfilingEventContext> getEventContextFilter() {
    // Events without a tracing context are excluded because they do not add much information.
    return taskSchedulingProfilingEventContext -> taskSchedulingProfilingEventContext.getTaskTracingContext().isPresent();
  }

  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }

}
