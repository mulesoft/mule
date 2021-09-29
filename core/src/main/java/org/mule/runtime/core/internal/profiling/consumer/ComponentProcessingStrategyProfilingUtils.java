/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for profiling
 */
public class ComponentProcessingStrategyProfilingUtils {

  public static final String PROFILING_EVENT_TIMESTAMP_KEY = "profilingEventTimestamp";
  public static final String PROCESSING_THREAD_KEY = "processingThread";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String RUNTIME_CORE_EVENT_CORRELATION_ID = "runtimeCoreEventCorrelationId";
  public static final String PROFILING_EVENT_TYPE = "profilingEventType";
  public static final String LOCATION = "location";
  public static final String COMPONENT_IDENTIFIER = "componentIdentifier";

  public static Map<String, String> getProcessingStrategyComponentInfoMap(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                                                                          ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put(PROFILING_EVENT_TYPE,
                 profilingEventType.getProfilingEventTypeNamespace() + ":"
                     + profilingEventType.getProfilingEventTypeIdentifier());
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, Long.toString(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getCorrelationId());
    profilingEventContext.getLocation().ifPresent(loc -> addLocationInfo(eventMap, loc));

    return eventMap;
  }

  public static Map<String, String> getComponentThreadingInfoMap(ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                                                                 ComponentThreadingProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    return eventMap;
  }

  private static void addLocationInfo(Map<String, String> eventMap, ComponentLocation location) {
    eventMap.put(LOCATION, location.getLocation());
    ComponentIdentifier identifier = location.getComponentIdentifier().getIdentifier();
    eventMap.put(COMPONENT_IDENTIFIER, identifier.getNamespace() + ":" + identifier.getName());
  }
}
