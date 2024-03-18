/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.api;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A factory for {@link SpanExporter}
 *
 * @since 4.5.0
 */
public interface SpanExporterFactory {

  /**
   * @param span            the {@link Span} to get the exporter for.
   * @param initialSpanInfo the {@link InitialSpanInfo}.
   *
   * @return a {@link SpanExporter} for the {@link Span}.
   */
  SpanExporter getSpanExporter(Span span, InitialSpanInfo initialSpanInfo);

  /**
   * @return a {@link SpanSnifferManager}.
   */
  SpanSnifferManager getSpanSnifferManager();
}
