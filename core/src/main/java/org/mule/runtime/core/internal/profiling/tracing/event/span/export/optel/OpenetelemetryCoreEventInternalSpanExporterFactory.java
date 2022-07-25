/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.getNewExportedSpanCapturer;

import static java.lang.System.getProperty;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExporterFactory;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.OpentelemetrySpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.SpanExporterConfiguration;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import io.opentelemetry.api.trace.Tracer;

/**
 * A {@link InternalSpanExporterFactory} for exporting spans associated to events.
 *
 * @since 4.5.0
 */
public class OpenetelemetryCoreEventInternalSpanExporterFactory implements InternalSpanExporterFactory<EventContext> {

  private static final SpanExporterConfiguration CONFIGURATION = new SystemPropertiesSpanExporterConfiguration();

  private static final Tracer TRACER = OpenTelemetryResourcesProvider.getOpenTelemetryTracer(CONFIGURATION);
  private static OpenetelemetryCoreEventInternalSpanExporterFactory instance;

  private OpenetelemetryCoreEventInternalSpanExporterFactory() {}

  public static OpenetelemetryCoreEventInternalSpanExporterFactory getOpenetelemetryCoreEventInternalSpanExporterFactory() {
    if (instance == null) {
      instance = new OpenetelemetryCoreEventInternalSpanExporterFactory();
    }

    return instance;
  }

  @Override
  public InternalSpanExporter from(EventContext eventContext, InternalSpan internalSpan) {
    return new OpentelemetrySpanExporter(TRACER, eventContext, internalSpan);
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
