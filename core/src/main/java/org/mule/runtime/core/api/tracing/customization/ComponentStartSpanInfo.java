/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link StartSpanInfo} based on the component.
 *
 * @since 4.5.0
 */
public class ComponentStartSpanInfo implements StartSpanInfo {


  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlation.id";
  public static final String THREAD_START_ID_KEY = "thread.start.id";
  public static final String THREAD_START_NAME_KEY = "thread.start.name";

  protected final CoreEvent coreEvent;
  protected final Component component;
  private String suffix = "";

  public ComponentStartSpanInfo(Component component,
                                CoreEvent coreEvent,
                                String suffix) {
    this.component = component;
    this.coreEvent = coreEvent;
    this.suffix = suffix;
  }

  public ComponentStartSpanInfo(Component component,
                                CoreEvent coreEvent) {
    this.component = component;
    this.coreEvent = coreEvent;
  }

  @Override
  public Map<String, String> getStartAttributes() {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(LOCATION_KEY, getLocationAsString(coreEvent));
    attributes.put(CORRELATION_ID_KEY, coreEvent.getCorrelationId());
    attributes.put(THREAD_START_ID_KEY, Long.toString(Thread.currentThread().getId()));
    attributes.put(THREAD_START_NAME_KEY, Thread.currentThread().getName());
    addLoggingVariablesAsAttributes(coreEvent, attributes);
    return attributes;
  }

  protected String getLocationAsString(CoreEvent coreEvent) {
    return SpanStartUtils.getLocationAsString(component.getLocation());
  }


  private void addLoggingVariablesAsAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    if (coreEvent instanceof PrivilegedEvent) {
      Optional<Map<String, String>> loggingVariables = ((PrivilegedEvent) coreEvent).getLoggingVariables();
      if (loggingVariables.isPresent()) {
        for (Map.Entry<String, String> entry : ((PrivilegedEvent) coreEvent).getLoggingVariables().get().entrySet()) {
          attributes.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @Override
  public String getName() {
    return SpanStartUtils.getSpanName(component.getIdentifier()) + suffix;
  }

  @Override
  public StartExportInfo getStartExportInfo() {
    return new StartExportInfo() {

      @Override
      public boolean isExportable() {
        return true;
      }
    };
  }

  @Override
  public boolean isPolicySpan() {
    if (component.getIdentifier() != null && component.getIdentifier().getName() != null
        && component.getIdentifier().getName().equals("execute-next")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean isRootSpan() {
    if (component.getIdentifier() != null && component.getIdentifier().getName() != null
        && component.getIdentifier().getName().equals("flow")) {
      return true;
    } ;

    return false;
  }
}
