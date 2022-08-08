/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getLocationAsString;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizer;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComponentSpanCustomizer implements SpanCustomizer {

  private Component component;
  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String ARTIFACT_TYPE_ID = "artifactType";
  public static final String THREAD_START_ID_KEY = "threadStartId";
  public static final String THREAD_START_NAME_KEY = "threadStartName";

  public ComponentSpanCustomizer(Component component) {
    this.component = component;
  }

  @Override
  public String getName(CoreEvent coreEvent) {
    return getSpanName(component.getIdentifier());
  }

  @Override
  public Map<String, String> getAttributes(CoreEvent coreEvent,
                                           MuleConfiguration muleConfiguration, ArtifactType artifactType) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(LOCATION_KEY, getLocationAsString(component.getLocation()));
    attributes.put(CORRELATION_ID_KEY, coreEvent.getCorrelationId());
    attributes.put(ARTIFACT_ID_KEY, muleConfiguration.getId());
    attributes.put(ARTIFACT_TYPE_ID, artifactType.getAsString());
    attributes.put(THREAD_START_ID_KEY, Long.toString(Thread.currentThread().getId()));
    attributes.put(THREAD_START_NAME_KEY, Thread.currentThread().getName());
    addLogggingVariablesAsAttributes(coreEvent, attributes);
    return attributes;
  }

  @Override
  public ChildSpanCustomizer getChildSpanCustomizer() {
    return ChildSpanCustomizerResolver.getChildSpanCustomizer(component);
  }

  private void addLogggingVariablesAsAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    if (coreEvent instanceof PrivilegedEvent) {
      Optional<Map<String, String>> loggingVariables = ((PrivilegedEvent) coreEvent).getLoggingVariables();
      if (loggingVariables.isPresent()) {
        for (Map.Entry<String, String> entry : ((PrivilegedEvent) coreEvent).getLoggingVariables().get().entrySet()) {
          attributes.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  static class ChildSpanCustomizerResolver {

    public static ChildSpanCustomizer getChildSpanCustomizer(Component component) {
      if (component.getIdentifier().getName().equals("until-successful")) {
        return new DefaultChildSpanCustomizer(":try");
      }
      return new DefaultChildSpanCustomizer(":route");
    }
  }
}
