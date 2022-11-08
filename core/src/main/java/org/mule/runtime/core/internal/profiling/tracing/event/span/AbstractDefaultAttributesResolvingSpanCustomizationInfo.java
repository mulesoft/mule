/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mule.runtime.core.privileged.util.MapUtils.mapWithKeysAndValues;

/**
 * A {@link SpanCustomizationInfo} that resolves the attributes from the {@link CoreEvent} with a default behaviour.
 *
 * @since 4.5.0
 */
public abstract class AbstractDefaultAttributesResolvingSpanCustomizationInfo implements SpanCustomizationInfo {

  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_ID = "artifactType";
  public static final String THREAD_START_ID_KEY = "threadStartId";
  public static final String THREAD_START_NAME_KEY = "threadStartName";
  public static final String[] KEYS =
      {LOCATION_KEY, CORRELATION_ID_KEY, ARTIFACT_ID_KEY, ARTIFACT_TYPE_ID, THREAD_START_ID_KEY, THREAD_START_NAME_KEY};

  @Override
  public Map<String, String> getAttributes(CoreEvent coreEvent, MuleConfiguration muleConfiguration, ArtifactType artifactType) {
    Map<String, String> map = mapWithLoggingVariables(coreEvent);
    Map<String, String> attributes = mapWithKeysAndValues(HashMap.class, KEYS,
                                                          new String[] {getLocationAsString(coreEvent),
                                                              coreEvent.getCorrelationId(), muleConfiguration.getId(),
                                                              artifactType.getAsString(),
                                                              Long.toString(Thread.currentThread().getId()),
                                                              Thread.currentThread().getName()});
    map.putAll(attributes);
    return map;
  }

  /**
   * @param coreEvent {@link CoreEvent} the coreEvent associated to the span.
   *
   * @return the location representation as a string
   */
  public abstract String getLocationAsString(CoreEvent coreEvent);


  private Map<String, String> mapWithLoggingVariables(CoreEvent coreEvent) {
    Map<String, String> attributes = new HashMap<>();
    if (coreEvent instanceof PrivilegedEvent) {
      Optional<Map<String, String>> loggingVariables = ((PrivilegedEvent) coreEvent).getLoggingVariables();
      if (loggingVariables.isPresent()) {
        attributes.putAll(loggingVariables.get());
      }
    }

    return attributes;
  }
}
