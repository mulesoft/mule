/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.tracer.api.span.exporter.SpanExporter.NOOP_EXPORTER;

import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

/**
 * A noop {@link SpanExporterFactory}.
 *
 * @since 4.5.0
 */
public class NoopSpanExporterFactory implements SpanExporterFactory {

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, InitialSpanInfo initialSpanInfo) {
    return NOOP_EXPORTER;
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return new NoOpSpanSnifferManager();
  }

  private static class NoOpSpanSnifferManager implements SpanSnifferManager {

  }
}
