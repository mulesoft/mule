/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.info;

import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getLocationAsString;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getSpanName;

import static org.apache.commons.lang3.StringUtils.defaultString;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.export.ExecutionInitialExportInfo;
import org.mule.runtime.tracer.customization.impl.export.TracingLevelExportInfo;
import org.mule.runtime.tracer.customization.impl.provider.DebugInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.OverviewInitialExportInfoProvider;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.function.BiConsumer;

/**
 * An implementation of {@link InitialSpanInfo} for the execution.
 *
 * @since 4.5.0
 */
public class ExecutionInitialSpanInfo implements InitialSpanInfo {

  public static final String API_ID_ATTRIBUTE_KEY = "api.id";

  public static final String LOCATION_KEY = "location";

  public static final int INITIAL_ATTRIBUTES_BASE_COUNT = 1;

  public static final String EXECUTE_NEXT = "execute-next";
  public static final String FLOW = "flow";
  private final Component component;
  private final String overriddenName;
  private final String spanNameSuffix;

  private InitialExportInfo initialExportInfo;

  private String name;
  private final boolean isPolicySpan;
  private final boolean rootSpan;
  private final String location;
  private final String apiId;
  private int initialAttributesCount = INITIAL_ATTRIBUTES_BASE_COUNT;

  public ExecutionInitialSpanInfo(Component component, String apiId,
                                  String overriddenName, String spanNameSuffix,
                                  TracingLevelConfiguration tracingLevelConfiguration) {

    this.location = getLocationAsString(component.getLocation());
    this.component = component;
    this.overriddenName = overriddenName;
    this.spanNameSuffix = spanNameSuffix;
    TracingLevelExportInfo tracingLevelExportInfo =
        getTracingLevelExportInfo(location, component, this.overriddenName, this.spanNameSuffix, tracingLevelConfiguration);
    this.initialExportInfo = resolveInitialExporterInfo(tracingLevelExportInfo);
    this.isPolicySpan = isComponentOfName(component, EXECUTE_NEXT) || component instanceof PolicyChain
        || name.equals(OPERATION_EXECUTION_SPAN_NAME);
    this.rootSpan = isComponentOfName(component, FLOW);

    this.apiId = apiId;
    if (apiId != null) {
      this.initialAttributesCount = INITIAL_ATTRIBUTES_BASE_COUNT + 1;
    }
  }

  public void reconfigureInitialSpanInfo(TracingLevelConfiguration tracingLevelConfiguration) {
    this.initialExportInfo =
        resolveInitialExporterInfo(getTracingLevelExportInfo(location, component, overriddenName, spanNameSuffix,
                                                             tracingLevelConfiguration));
  }

  private static ExecutionInitialExportInfo resolveInitialExporterInfo(TracingLevelExportInfo tracingLevelExportInfo) {
    return new ExecutionInitialExportInfo(tracingLevelExportInfo);
  }

  private TracingLevelExportInfo getTracingLevelExportInfo(String location, Component component, String overriddenName,
                                                           String spanNameSuffix,
                                                           TracingLevelConfiguration tracingLevelConfiguration) {
    TracingLevel tracingLevel = tracingLevelConfiguration.getTracingLevel();
    TracingLevel tracingLevelOverride = tracingLevelConfiguration.getTracingLevelOverride(location);
    TracingLevelExportInfo tracingLevelExportInfo;
    if (!tracingLevelOverride.equals(tracingLevel)) {
      tracingLevelExportInfo = new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevelOverride), true);
    } else {
      tracingLevelExportInfo =
          new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevel), false);
    }

    if (overriddenName == null) {
      name = getSpanName(component.getIdentifier()) + defaultString(spanNameSuffix);
      tracingLevelExportInfo.setSpanIdentifier(component);
    } else {
      name = overriddenName;
      tracingLevelExportInfo.setSpanIdentifier(name);
    }

    return tracingLevelExportInfo;
  }

  private InitialExportInfoProvider resolveInitialExportInfoProvider(TracingLevel tracingLevel) {
    switch (tracingLevel) {
      case OVERVIEW:
        return new OverviewInitialExportInfoProvider();
      case DEBUG:
        return new DebugInitialExportInfoProvider();
      default:
        return new MonitoringInitialExportInfoProvider();
    }
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    biConsumer.accept(LOCATION_KEY, location);
    if (apiId != null) {
      biConsumer.accept(API_ID_ATTRIBUTE_KEY, apiId);
    }
  }

  @Override
  public int getInitialAttributesCount() {
    return initialAttributesCount;
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
