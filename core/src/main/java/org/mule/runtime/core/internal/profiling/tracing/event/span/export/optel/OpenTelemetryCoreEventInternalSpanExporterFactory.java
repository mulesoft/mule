/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.getNewExportedSpanCapturer;

import static java.lang.System.getProperty;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.getOpenTelemetryTracer;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.OpenTelemetrySpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.SpanExporterConfiguration;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

/**
 * A factory for exporting spans associated to events.
 *
 * @since 4.5.0
 */
public class OpenTelemetryCoreEventInternalSpanExporterFactory {

  private static final SpanExporterConfiguration CONFIGURATION = new SystemPropertiesSpanExporterConfiguration();

  private static OpenTelemetryCoreEventInternalSpanExporterFactory instance;

  private OpenTelemetryCoreEventInternalSpanExporterFactory() {}

  public static OpenTelemetryCoreEventInternalSpanExporterFactory getOpenTelemetryCoreEventInternalSpanExporterFactory() {
    if (instance == null) {
      instance = new OpenTelemetryCoreEventInternalSpanExporterFactory();
    }

    return instance;
  }

  /**
   * @param eventContext an extra instance that may have extra information for creat
   *
   * @param internalSpan the {@link InternalSpan} that will eventually be exported
   * @return the result exporter.
   */
  public InternalSpanExporter from(EventContext eventContext, MuleConfiguration muleConfiguration, InternalSpan internalSpan) {
    return new OpenTelemetrySpanExporter(getOpenTelemetryTracer(CONFIGURATION, muleConfiguration.getId()), eventContext,
                                         internalSpan);
  }

  public ExportedSpanCapturer getExportedSpanCapturer() {
    return getNewExportedSpanCapturer();
  }

  /**
   * A {@link SpanExporterConfiguration} based on system properties.
   */
  private static class SystemPropertiesSpanExporterConfiguration implements SpanExporterConfiguration {

    private SystemPropertiesSpanExporterConfiguration() {}

    @Override
    public String getValue(String key) {
      return getProperty(key);
    }
  }
}
