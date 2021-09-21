/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.COMPONENT_THREAD_RELEASE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentExecutionProfilingEventContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.Gson;
import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that logs information regarding {@link Component} executions.
 */
public class LoggerComponentExecutionDataConsumer
    implements ProfilingDataConsumer<ComponentExecutionProfilingEventContext> {

  private static final Logger LOGGER = getLogger(LoggerComponentExecutionDataConsumer.class);

  public static final String PROFILING_EVENT_TIMESTAMP_KEY = "profilingEventTimestamp";
  public static final String PROCESSING_THREAD_KEY = "processingThread";
  public static final String PROCESSING_THREAD_STATE = "processingThreadState";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String RUNTIME_CORE_EVENT_CORRELATION_ID = "runtimeCoreEventCorrelationId";
  public static final String PROFILING_EVENT_TYPE = "profilingEventType";
  public static final String LOCATION = "location";

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType,
                               ComponentExecutionProfilingEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getInfoMap(profilingEventType, profilingEventContext)));
    }
  }

  private Map<String, Object> getInfoMap(ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType,
                                         ComponentExecutionProfilingEventContext profilingEventContext) {
    Map<String, Object> eventMap = new HashMap<>();
    eventMap.put(PROFILING_EVENT_TYPE,
                 profilingEventType.getProfilingEventTypeNamespace() + ":"
                     + profilingEventType.getProfilingEventTypeIdentifier());
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, Long.toString(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getCorrelationId());
    profilingEventContext.getThreadSnapshot().ifPresent(threadSnapshot -> eventMap.put(PROCESSING_THREAD_STATE, threadSnapshot));
    profilingEventContext.getLocation().ifPresent(location -> eventMap.put(LOCATION, location.getLocation()));

    return eventMap;
  }

  @Override
  public Set<ProfilingEventType<ComponentExecutionProfilingEventContext>> getProfilingEventTypes() {
    return of(PS_SCHEDULING_OPERATION_EXECUTION, STARTING_OPERATION_EXECUTION, OPERATION_EXECUTED,
              PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
              FLOW_EXECUTED, COMPONENT_THREAD_RELEASE);
  }

  @Override
  public Predicate<ComponentExecutionProfilingEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }

  /**
   * @return the logger used for consuming the profiling data.
   */
  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }
}
