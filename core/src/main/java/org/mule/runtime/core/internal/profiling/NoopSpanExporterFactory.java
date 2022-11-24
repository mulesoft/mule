/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import static org.mule.runtime.tracer.api.span.exporter.SpanExporter.NOOP_EXPORTER;

public class NoopSpanExporterFactory implements SpanExporterFactory {

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, StartSpanInfo startExportInfo) {
    return NOOP_EXPORTER;
  }

  @Override
  public SpanSnifferManager getSpanExporterManager() {
    return new NoOpSpanSnifferManager();
  }

  private static class NoOpSpanSnifferManager implements SpanSnifferManager {

  }
}
