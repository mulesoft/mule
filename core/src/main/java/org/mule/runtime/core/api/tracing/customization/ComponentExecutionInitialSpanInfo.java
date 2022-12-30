/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import static org.mule.runtime.core.api.tracing.customization.SpanInitialInfoUtils.getLocationAsString;
import static org.mule.runtime.core.api.tracing.customization.SpanInitialInfoUtils.getSpanName;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.function.BiConsumer;

/**
 * A {@link InitialSpanInfo} based on a component.
 *
 * @since 4.5.0
 */
public class ComponentExecutionInitialSpanInfo implements InitialSpanInfo {


  public static final String LOCATION_KEY = "location";
  // These are location, correlation.id, thread.start.id, thread.start.name
  public static final int INITIAL_ATTRIBUTES_BASE_COUNT = 1;
  public static final String EXECUTE_NEXT = "execute-next";
  public static final String FLOW = "flow";

  protected final Component component;
  private final String name;
  private final boolean isPolicySpan;
  private final boolean rootSpan;

  public ComponentExecutionInitialSpanInfo(Component component,
                                           String spanNameSuffix) {
    this.component = component;
    this.name = getSpanName(component.getIdentifier()) + spanNameSuffix;
    this.isPolicySpan = isComponentOfName(component, EXECUTE_NEXT);
    this.rootSpan = isComponentOfName(component, FLOW);

  }

  public ComponentExecutionInitialSpanInfo(Component component) {
    this(component, "");
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    biConsumer.accept(LOCATION_KEY, getLocationAsString(component.getLocation()));
  }

  @Override
  public int getInitialAttributesCount() {
    return INITIAL_ATTRIBUTES_BASE_COUNT;
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
    return isPolicySpan;
  }

  @Override
  public boolean isRootSpan() {
    return rootSpan;
  }


  private boolean isComponentOfName(Component component, String name) {
    return component.getIdentifier() != null && name.equals(component.getIdentifier().getName());
  }
}
