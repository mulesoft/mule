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
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;

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
  public static final String TX_ACTION = "action";
  public static final String TX_TYPE = "type";
  public static final String TX_CREATION_LOCATION = "createdIn";
  public static final String TX_CURRENT_LOCATION = "actionIn";

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

  private static void addThreadingData(ComponentThreadingProfilingEventContext profilingEventContext,
                                       Map<String, String> eventMap) {
    profilingEventContext.getThreadSnapshot().ifPresent(threadSnapshot -> addThreadSnapshotInfo(eventMap, threadSnapshot));
  }

  private static void addThreadSnapshotInfo(Map<String, String> eventMap, ThreadSnapshot threadSnapshot) {
    eventMap.put(BLOCKED_TIME_KEY, valueOf(threadSnapshot.getBlockedTime()));
    eventMap.put(WAITED_TIME_KEY, valueOf(threadSnapshot.getWaitedTime()));
    eventMap.put(CPU_TIME_KEY, valueOf(threadSnapshot.getCpuTime()));
  }

  private static void addComponentData(ProfilingEventType<? extends ComponentProfilingEventContext> profilingEventType,
                                       ComponentProfilingEventContext profilingEventContext, Map<String, String> eventMap) {
    eventMap.put(PROFILING_EVENT_TYPE, format("%s:%s", profilingEventType.getProfilingEventTypeNamespace(),
                                              profilingEventType.getProfilingEventTypeIdentifier()));
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, valueOf(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getCorrelationId());
    profilingEventContext.getLocation().ifPresent(loc -> addLocationInfo(eventMap, loc));
  }


  private static void addLocationInfo(Map<String, String> eventMap, ComponentLocation location) {
    eventMap.put(LOCATION, location.getLocation());
    ComponentIdentifier identifier = location.getComponentIdentifier().getIdentifier();
    eventMap.put(COMPONENT_IDENTIFIER, format("%s:%s", identifier.getNamespace(), identifier.getName()));
  }

  public static Map<String, String> getTxInfo(ProfilingEventType<TransactionProfilingEventContext> profilingEventType,
                                              TransactionProfilingEventContext profilingEventContext) {
    Map<String, String> info = new HashMap<>();
    info.put(TX_ACTION, profilingEventType.toString());
    info.put(TX_TYPE, profilingEventContext.getType().toString());
    info.put(TX_CREATION_LOCATION, profilingEventContext.getTransactionOriginatingLocation());
    info.put(TX_CURRENT_LOCATION, profilingEventContext.getEventOrginatingLocation().getLocation());
    return info;
  }

}
