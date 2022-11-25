/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api;

import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

/**
 * A factory for {@link SpanExporter}
 *
 * @since 4.5.0
 */
public interface SpanExporterFactory {

  /**
   * @param internalSpan    the {@link InternalSpan} to get the exporter for.
   * @param startExportInfo the {@link StartExportInfo}.
   *
   * @return a {@link SpanExporter} for the {@link InternalSpan}.
   */
  SpanExporter getSpanExporter(InternalSpan internalSpan, StartSpanInfo startExportInfo);

  /**
   * @return a {@link SpanSnifferManager}.
   */
  SpanSnifferManager getSpanExporterManager();
}
