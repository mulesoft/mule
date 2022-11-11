/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling.tracing;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils class for tracing testing.
 */
public class TracingTestUtils {

  public static final String LOCATION_KEY = "location";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_ID = "artifactType";

  public static Map<String, String> createAttributeMap(String location,
                                                       String artifactId) {
    Map<String, String> attributeMap = new HashMap<>();
    attributeMap.put(ARTIFACT_ID_KEY, artifactId);
    attributeMap.put(LOCATION_KEY, location);
    attributeMap.put(ARTIFACT_TYPE_ID, APP.getAsString());
    return attributeMap;
  }
}
