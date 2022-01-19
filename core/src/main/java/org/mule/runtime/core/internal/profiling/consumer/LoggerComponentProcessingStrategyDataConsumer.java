/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getProcessingStrategyComponentInfoMap;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;

import com.google.gson.Gson;

import java.util.Set;
import java.util.function.Predicate;

import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that logs information regarding the processing strategy for components.
 */
@RuntimeInternalProfilingDataConsumer
public class LoggerComponentProcessingStrategyDataConsumer
    implements ProfilingDataConsumer<ComponentProcessingStrategyProfilingEventContext> {

  private static final Logger LOGGER = getLogger(LoggerComponentProcessingStrategyDataConsumer.class);

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                               ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getProcessingStrategyComponentInfoMap(profilingEventType, profilingEventContext)));
    }
  }

  @Override
  public Set<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>> getProfilingEventTypes() {
    return of(PS_SCHEDULING_OPERATION_EXECUTION, PS_STARTING_OPERATION_EXECUTION, PS_OPERATION_EXECUTED,
              PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
              FLOW_EXECUTED);
  }

  @Override
  public Predicate<ComponentProcessingStrategyProfilingEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }

  /**
   * @return the logger used for consuming the profiling data.
   */
  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }
}
