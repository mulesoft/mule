/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.info;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.getLocationAsString;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.getSpanName;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;

import java.util.function.BiConsumer;

/**
 * An implementation of {@link InitialSpanInfo} for the execution.
 *
 * @since 4.6.0
 */
public class ExecutionInitialSpanInfo implements InitialSpanInfo {

  public static final String LOCATION_KEY = "location";

  public static final int INITIAL_ATTRIBUTES_BASE_COUNT = 1;

  public static final String EXECUTE_NEXT = "execute-next";
  public static final String FLOW = "flow";
  public static final String NO_LOCATION = "no-location";

  private final InitialExportInfo initialExportInfo;

  private final String name;
  private final boolean isPolicySpan;
  private final boolean rootSpan;
  private final String location;

  public ExecutionInitialSpanInfo(Component component, InitialExportInfoProvider initialExportInfoProvider) {
    this(component, initialExportInfoProvider, null, "");
  }

  public ExecutionInitialSpanInfo(Component component, String overriddenName,
                                  InitialExportInfoProvider initialExportInfoProvider) {
    this(component, initialExportInfoProvider, overriddenName, "");
  }

  public ExecutionInitialSpanInfo(Component component, InitialExportInfoProvider initialExportInfoProvider,
                                  String overriddenName, String spanNameSuffix) {
    initialExportInfo = initialExportInfoProvider.getInitialExportInfo(component);
    if (overriddenName == null) {
      name = getSpanName(component.getIdentifier()) + spanNameSuffix;
    } else {
      name = overriddenName;
    }
    this.isPolicySpan = isComponentOfName(component, EXECUTE_NEXT) || component instanceof PolicyChain;
    this.rootSpan = isComponentOfName(component, FLOW);
    this.location = getLocationAsString(component.getLocation());
  }

  public ExecutionInitialSpanInfo(String name, InitialExportInfoProvider initialExportInfoProvider) {
    this(name, initialExportInfoProvider, "");
  }

  public ExecutionInitialSpanInfo(String name, InitialExportInfoProvider initialExportInfoProvider, String spanNameSuffix) {
    initialExportInfo = initialExportInfoProvider.getInitialExportInfo(name + stripToEmpty(spanNameSuffix));
    this.name = name;
    this.isPolicySpan = false;
    this.rootSpan = false;
    this.location = NO_LOCATION;
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    biConsumer.accept(LOCATION_KEY, location);
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
    return initialExportInfo;
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
