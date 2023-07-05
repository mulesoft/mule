/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.DebugInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.OverviewInitialExportInfoProvider;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.Set;

// Hacer que esto implemente InitialExportInfo
public class TracingLevelExportInfo {

  private final Component component;
  // Tratar de unificar estos dos valores de alguna manera
  private final String spanNameOverride;
  private InitialExportInfoProvider initialExportInfoProvider;
  private final boolean isLevelOverride;

  private TracingLevelExportInfo(InitialExportInfoProvider initialExportInfoProvider, String spanNameOverride,
                                 boolean isLevelOverride) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.isLevelOverride = isLevelOverride;
    this.spanNameOverride = spanNameOverride;
    this.component = null;
  }

  private TracingLevelExportInfo(InitialExportInfoProvider initialExportInfoProvider, Component component,
                                 boolean isLevelOverride) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.isLevelOverride = isLevelOverride;
    this.component = component;
    this.spanNameOverride = null;
  }

  public static TracingLevelExportInfo createTracingLevelExportInfo(Component component,
                                                                    TracingLevelConfiguration tracingLevelConfiguration) {
    // Porque se pide un override si puede ser igual al level? Agregar un isOverride a TracingLevel
    TracingLevel tracingLevel = tracingLevelConfiguration.getTracingLevel();
    TracingLevel tracingLevelOverride = getTracingLevelOverride(component, tracingLevelConfiguration);
    return new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevelOverride), component,
                                      !tracingLevelOverride.equals(tracingLevel));
  }

  public static TracingLevelExportInfo createTracingLevelExportInfo(Component component, String nameOverride,
                                                                    TracingLevelConfiguration tracingLevelConfiguration) {
    // Porque se pide un override si puede ser igual al level? Agregar un isOverride a TracingLevel
    TracingLevel tracingLevel = tracingLevelConfiguration.getTracingLevel();
    TracingLevel tracingLevelOverride =
        getTracingLevelOverride(component, tracingLevelConfiguration);
    return new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevelOverride), nameOverride,
                                      !tracingLevelOverride.equals(tracingLevel));
  }

  private static TracingLevel getTracingLevelOverride(Component component, TracingLevelConfiguration tracingLevelConfiguration) {
    return component.getLocation() != null
        ? tracingLevelConfiguration.getTracingLevelOverride(component.getLocation().getLocation())
        : tracingLevelConfiguration.getTracingLevel();
  }

  private static InitialExportInfoProvider resolveInitialExportInfoProvider(TracingLevel tracingLevel) {
    switch (tracingLevel) {
      case OVERVIEW:
        return new OverviewInitialExportInfoProvider();
      case DEBUG:
        return new DebugInitialExportInfoProvider();
      default:
        return new MonitoringInitialExportInfoProvider();
    }
  }

  // Esto tendria que ser mas estatico (revisar)
  private InitialExportInfo getInitialExportInfo() {
    if (spanNameOverride != null) {
      return initialExportInfoProvider.getInitialExportInfo(spanNameOverride);
    } else {
      return initialExportInfoProvider.getInitialExportInfo(component);
    }
  }

  public boolean isExportable() {
    return getInitialExportInfo().isExportable();
  }

  public Set<String> noExportUntil() {
    return getInitialExportInfo().noExportUntil();
  }

  public void initialize(InitialExportInfo initialExportInfo) {
    if (!isLevelOverride) {
      this.initialExportInfoProvider =
          ((ExecutionInitialExportInfo) initialExportInfo).getTracingLevelExportInfo().getInitialExportInfoProvider();
    }
  }

  public InitialExportInfoProvider getInitialExportInfoProvider() {
    return initialExportInfoProvider;
  }
}
