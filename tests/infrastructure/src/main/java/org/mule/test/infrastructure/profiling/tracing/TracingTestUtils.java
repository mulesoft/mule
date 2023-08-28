/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.profiling.tracing;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils class for tracing testing.
 */
public class TracingTestUtils {

  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlation.id";
  public static final String THREAD_START_ID_KEY = "thread.start.id";
  public static final String THREAD_END_NAME_KEY = "thread.end.name";
  public static final String ARTIFACT_ID_KEY = "artifact.id";
  public static final String ARTIFACT_TYPE_ID = "artifact.type";

  public static Map<String, String> createAttributeMap(String location,
                                                       String artifactId) {
    Map<String, String> attributeMap = new HashMap<>();
    attributeMap.put(ARTIFACT_ID_KEY, artifactId);
    attributeMap.put(LOCATION_KEY, location);
    attributeMap.put(ARTIFACT_TYPE_ID, APP.getAsString());
    return attributeMap;
  }

  public static List<String> getDefaultAttributesToAssertExistence() {
    return Arrays.asList(CORRELATION_ID_KEY, THREAD_START_ID_KEY, THREAD_END_NAME_KEY);
  }
}
