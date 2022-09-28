/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryCoreEventInternalSpanExporterFactory.getOpenTelemetryCoreEventInternalSpanExporterFactory;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.Set;

/**
 * A {@link InternalSpanExportManager} that exports the span as open telemetry.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterManager implements InternalSpanExportManager<EventContext> {

  private static final OpenTelemetryCoreEventInternalSpanExporterFactory SPAN_EXPORTER_FACTORY =
      getOpenTelemetryCoreEventInternalSpanExporterFactory();

  @Override
  public ExportedSpanCapturer getExportedSpanCapturer() {
    return SPAN_EXPORTER_FACTORY.getExportedSpanCapturer();
  }

  @Override
  public InternalSpanExporter getInternalSpanExporter(EventContext context, MuleConfiguration muleConfiguration,
                                                      boolean exportable,
                                                      Set<String> noExportUntil,
                                                      InternalSpan internalSpan) {
    return SPAN_EXPORTER_FACTORY.from(context, muleConfiguration, exportable, noExportUntil,
                                      internalSpan);
  }
}
