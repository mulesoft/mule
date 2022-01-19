/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_THREAD_RELEASE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getComponentThreadingInfoMap;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.gson.Gson;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link ProfilingDataConsumer} that logs information regarding the processing strategy for components.
 */
@RuntimeInternalProfilingDataConsumer
public class LoggerComponentThreadingDataConsumer
    implements ProfilingDataConsumer<ComponentThreadingProfilingEventContext> {

  private static final Logger LOGGER = getLogger(LoggerComponentThreadingDataConsumer.class);

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                               ComponentThreadingProfilingEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getComponentThreadingInfoMap(profilingEventType, profilingEventContext)));
    }
  }

  @Override
  public Set<ProfilingEventType<ComponentThreadingProfilingEventContext>> getProfilingEventTypes() {
    return of(STARTING_OPERATION_EXECUTION, OPERATION_THREAD_RELEASE, OPERATION_EXECUTED);
  }

  @Override
  public Predicate<ComponentThreadingProfilingEventContext> getEventContextFilter() {
    return profilingEventContext -> true;
  }

  /**
   * @return the logger used for consuming the profiling data.
   */
  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }
}
