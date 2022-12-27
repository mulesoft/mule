/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link InitialSpanInfo} based on a component.
 *
 * @since 4.5.0
 */
public class ComponentExecutionInitialSpanInfo implements InitialSpanInfo {


  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlation.id";
  public static final String THREAD_START_ID_KEY = "thread.start.id";
  public static final String THREAD_START_NAME_KEY = "thread.start.name";

  protected final CoreEvent coreEvent;
  protected final Component component;
  private final String name;

  public ComponentExecutionInitialSpanInfo(Component component,
                                           CoreEvent coreEvent,
                                           String spanNameSuffix) {
    this.component = component;
    this.coreEvent = coreEvent;
    this.name = SpanInitialInfoUtils.getSpanName(component.getIdentifier()) + spanNameSuffix;
  }

  public ComponentExecutionInitialSpanInfo(Component component,
                                           CoreEvent coreEvent) {
    this.component = component;
    this.coreEvent = coreEvent;
    this.name = SpanInitialInfoUtils.getSpanName(component.getIdentifier());
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    biConsumer.accept(LOCATION_KEY, getLocationAsString(coreEvent));
    biConsumer.accept(CORRELATION_ID_KEY, coreEvent.getCorrelationId());
    biConsumer.accept(THREAD_START_NAME_KEY, Thread.currentThread().getName());
    biConsumer.accept(THREAD_START_ID_KEY, Long.toString(Thread.currentThread().getId()));
    if (coreEvent instanceof PrivilegedEvent) {
      ((PrivilegedEvent) coreEvent).getLoggingVariables().ifPresent(loggingVariables -> loggingVariables.forEach(biConsumer));
    }
  }

  @Override
  public int getInitialAttributesCount() {
    int count = 4;

    if (coreEvent instanceof PrivilegedEvent) {
      count += ((PrivilegedEvent) coreEvent).getLoggingVariables().orElse(emptyMap()).size();
    }

    return count;
  }

  protected String getLocationAsString(CoreEvent coreEvent) {
    return SpanInitialInfoUtils.getLocationAsString(component.getLocation());
  }


  private void addLoggingVariablesAsAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    if (coreEvent instanceof PrivilegedEvent) {
      attributes.putAll(((PrivilegedEvent) coreEvent).getLoggingVariables().orElse(emptyMap()));
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }

  @Override
  public boolean isPolicySpan() {
    return component.getIdentifier() != null && "execute-next".equals(component.getIdentifier().getName());
  }

  @Override
  public boolean isRootSpan() {
    return component.getIdentifier() != null && "flow".equals(component.getIdentifier().getName());
  }
}
