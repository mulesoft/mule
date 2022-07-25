/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenetelemetryCoreEventInternalSpanExporterFactory.getOpenetelemetryCoreEventInternalSpanExporterFactory;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

/**
 * A {@link InternalSpanExportManager} that exports the span as opentelemetry.
 *
 * @since 4.5.0
 */
public class OpentelemetrySpanExporterManager implements InternalSpanExportManager<EventContext> {

  private static final OpenetelemetryCoreEventInternalSpanExporterFactory SPAN_EXPORTER_FACTORY =
      getOpenetelemetryCoreEventInternalSpanExporterFactory();

  @Override
  public ExportedSpanCapturer getExportedSpanCapturer() {
    return SPAN_EXPORTER_FACTORY.getExportedSpanCapturer();
  }

  @Override
  public InternalSpanExporter getInternalSpanExporter(EventContext context, InternalSpan internalSpan) {
    return SPAN_EXPORTER_FACTORY.from(context, internalSpan);
  }
}
