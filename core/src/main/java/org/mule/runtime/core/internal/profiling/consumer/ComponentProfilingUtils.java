/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.threading.ThreadSnapshot;
import org.mule.runtime.api.profiling.tracing.TracingContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.TaskSchedulingProfilingEventContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for profiling
 */
public class ComponentProfilingUtils {

  public static final String PROFILING_EVENT_TIMESTAMP_KEY = "profilingEventTimestamp";
  public static final String PROCESSING_THREAD_KEY = "processingThread";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String RUNTIME_CORE_EVENT_CORRELATION_ID = "runtimeCoreEventCorrelationId";
  public static final String PROFILING_EVENT_TYPE = "profilingEventType";
  public static final String LOCATION = "location";
  public static final String COMPONENT_IDENTIFIER = "componentIdentifier";
  public static final String BLOCKED_TIME_KEY = "blockedTimeMillis";
  public static final String WAITED_TIME_KEY = "waitedTimeMillis";
  public static final String CPU_TIME_KEY = "cpuTimeNanos";
  public static final String TASK_ID_KEY = "taskId";

  private ComponentProfilingUtils() {}

  public static Map<String, String> getProcessingStrategyComponentInfoMap(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                                                                          ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    addComponentData(profilingEventType, profilingEventContext, eventMap);
    // TODO EE-8092: Add data related to the Processing Strategy here.
    return eventMap;
  }

  public static Map<String, String> getComponentThreadingInfoMap(ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                                                                 ComponentThreadingProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    addComponentData(profilingEventType, profilingEventContext, eventMap);
    addThreadingData(profilingEventContext, eventMap);
    return eventMap;
  }

  public static Map<String, String> getTaskSchedulingInfoMap(ProfilingEventType<TaskSchedulingProfilingEventContext> profilingEventType,
                                                             TaskSchedulingProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, valueOf(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(TASK_ID_KEY, profilingEventContext.getTaskId());
    addProfilingEventTypeData(profilingEventType, eventMap);
    addTracingContextData(profilingEventContext.getTaskTracingContext(), eventMap);
    return eventMap;
  }

  private static void addTracingContextData(TracingContext taskTracingContext, Map<String, String> eventMap) {
    taskTracingContext.getRunningComponentMetadata().ifPresent(componentMetadata -> {
      eventMap.put(ARTIFACT_ID_KEY, componentMetadata.getArtifactId());
      eventMap.put(ARTIFACT_TYPE_KEY, componentMetadata.getArtifactType());
      eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, componentMetadata.getCorrelationId());
      componentMetadata.getComponentLocation().ifPresent(componentLocation -> addLocationData(eventMap, componentLocation));
    });

  }

  private static void addThreadingData(ComponentThreadingProfilingEventContext profilingEventContext,
                                       Map<String, String> eventMap) {
    profilingEventContext.getThreadSnapshot().ifPresent(threadSnapshot -> addThreadSnapshotData(eventMap, threadSnapshot));
  }

  private static void addThreadSnapshotData(Map<String, String> eventMap, ThreadSnapshot threadSnapshot) {
    eventMap.put(BLOCKED_TIME_KEY, valueOf(threadSnapshot.getBlockedTime()));
    eventMap.put(WAITED_TIME_KEY, valueOf(threadSnapshot.getWaitedTime()));
    eventMap.put(CPU_TIME_KEY, valueOf(threadSnapshot.getCpuTime()));
  }

  private static void addComponentData(ProfilingEventType<? extends ComponentProfilingEventContext> profilingEventType,
                                       ComponentProfilingEventContext profilingEventContext, Map<String, String> eventMap) {
    addProfilingEventTypeData(profilingEventType, eventMap);
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, valueOf(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getCorrelationId());
    profilingEventContext.getLocation().ifPresent(loc -> addLocationData(eventMap, loc));
  }

  private static void addProfilingEventTypeData(ProfilingEventType<?> profilingEventType, Map<String, String> eventMap) {
    eventMap.put(PROFILING_EVENT_TYPE, format("%s:%s", profilingEventType.getProfilingEventTypeNamespace(),
                                              profilingEventType.getProfilingEventTypeIdentifier()));
  }

  private static void addLocationData(Map<String, String> eventMap, ComponentLocation location) {
    eventMap.put(LOCATION, location.getLocation());
    ComponentIdentifier identifier = location.getComponentIdentifier().getIdentifier();
    eventMap.put(COMPONENT_IDENTIFIER, format("%s:%s", identifier.getNamespace(), identifier.getName()));
  }

}
