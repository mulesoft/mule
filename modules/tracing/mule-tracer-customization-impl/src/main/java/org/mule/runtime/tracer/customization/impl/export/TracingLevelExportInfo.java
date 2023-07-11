/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils;
import org.mule.runtime.tracer.customization.impl.provider.DebugInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.provider.OverviewInitialExportInfoProvider;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.Set;

import static java.util.Collections.emptySet;

public class TracingLevelExportInfo implements InitialExportInfo {

  private final String spanName;
  private TracingLevel tracingLevel;
  private final LazyValue<InitialExportInfo> initialExportInfo = new LazyValue<>(this::getInitialExportInfo);
  private Set<String> propagatedNoExportUntil = emptySet();

  public TracingLevel getTracingLevel() {
    return tracingLevel;
  }

  private TracingLevelExportInfo(TracingLevel tracingLevel, String spanName) {
    this.tracingLevel = tracingLevel;
    this.spanName = spanName;
  }

  public static TracingLevelExportInfo createTracingLevelExportInfo(Component component, String spanName,
                                                                    TracingLevelConfiguration tracingLevelConfiguration) {
    return new TracingLevelExportInfo(tracingLevelConfiguration
        .getTracingLevel(SpanInitialInfoUtils.getLocationAsString(component.getLocation())), spanName);
  }

  private static InitialExportInfoProvider resolveInitialExportInfoProvider(TracingLevel tracingLevel) {
    switch (tracingLevel.getTracingLevelId()) {
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
    if (propagatedNoExportUntil.isEmpty() || propagatedNoExportUntil.contains(spanName)) {
      return resolveInitialExportInfoProvider(tracingLevel).getInitialExportInfo(spanName);
    } else {
      return new InitialExportInfo() {

        @Override
        public boolean isExportable() {
          return false;
        }

        @Override
        public Set<String> noExportUntil() {
          return propagatedNoExportUntil;
        }

        @Override
        public void propagateInitialExportInfo(InitialExportInfo initialExportInfo) {
          // Nothing to do
        }
      };
    }
  }

  public boolean isExportable() {
    return initialExportInfo.get().isExportable();
  }

  public Set<String> noExportUntil() {
    return initialExportInfo.get().noExportUntil();
  }

  @Override
  public void propagateInitialExportInfo(InitialExportInfo initialExportInfo) {
    // Tenemos que remover este class cast
    if (!tracingLevel.isOverride()) {
      this.tracingLevel =
          ((TracingLevelExportInfo) initialExportInfo).getTracingLevel();
    }
    this.propagatedNoExportUntil = initialExportInfo.noExportUntil();
  }

}
