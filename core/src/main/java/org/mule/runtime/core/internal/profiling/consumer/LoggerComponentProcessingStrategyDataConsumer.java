/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.FLOW_EXECUTED;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.profiling.consumer.context.ProcessingStrategyProfilingEventContext;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that logs information regarding the processing strategy for components.
 */
public class LoggerComponentProcessingStrategyDataConsumer
    implements ProfilingDataConsumer<ProcessingStrategyProfilingEventContext> {

  private static final Logger LOGGER = getLogger(LoggerComponentProcessingStrategyDataConsumer.class);

  public static final String PROFILING_EVENT_TIMESTAMP_KEY = "profilingEventTimestamp";
  public static final String PROCESSING_THREAD_KEY = "processingThread";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String RUNTIME_CORE_EVENT_CORRELATION_ID = "runtimeCoreEventCorrelationId";
  public static final String PROFILING_EVENT_TYPE = "profilingEventType";
  public static final String LOCATION = "location";

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType,
                               ProcessingStrategyProfilingEventContext profilingEventContext) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(gson.toJson(getInfoMap(profilingEventType, profilingEventContext)));
    }
  }

  private Map<String, String> getInfoMap(ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType,
                                         ProcessingStrategyProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put(PROFILING_EVENT_TYPE,
                 profilingEventType.getProfilingEventTypeNamespace() + ":"
                     + profilingEventType.getProfilingEventTypeIdentifier());
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, Long.toString(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getEvent().getCorrelationId());
    profilingEventContext.getLocation().map(loc -> eventMap.put(LOCATION, loc.getLocation()));

    return eventMap;
  }

  @Override
  public Set<ProfilingEventType<ProcessingStrategyProfilingEventContext>> getProfilingEventTypes() {
    return of(PS_SCHEDULING_OPERATION_EXECUTION, STARTING_OPERATION_EXECUTION, OPERATION_EXECUTED,
              PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
              FLOW_EXECUTED);
  }

  @Override
  public Predicate<ProcessingStrategyProfilingEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }
}
