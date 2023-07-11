/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static org.mule.runtime.tracer.exporter.impl.optel.sdk.OpenTelemetryInstrumentationConstants.INSTRUMENTATION_LIBRARY_INFO;

import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * An implementation of OTEL sdk {@link ReadableSpan}.
 *
 * @since 4.5.0
 */
public class MuleReadableSpan implements ReadableSpan {

  private final OpenTelemetrySpanExporter openTelemetrySpanExporter;
  private final MuleSpanData spanData;

  public MuleReadableSpan(OpenTelemetrySpanExporter openTelemetrySpanExporter, Resource resource, String artifactId,
                          String artifactType) {
    this.openTelemetrySpanExporter = openTelemetrySpanExporter;
    this.spanData = new MuleSpanData(this, resource, artifactId, artifactType);
  }

  @Override
  public SpanContext getSpanContext() {
    return openTelemetrySpanExporter.getSpanContext();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return openTelemetrySpanExporter.getParentSpanContext();
  }

  @Override
  public String getName() {
    return openTelemetrySpanExporter.getName();
  }

  @Override
  public SpanData toSpanData() {
    return spanData;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return INSTRUMENTATION_LIBRARY_INFO;
  }

  @Override
  public boolean hasEnded() {
    return true;
  }

  @Override
  public long getLatencyNanos() {
    return 0;
  }

  @Override
  public SpanKind getKind() {
    return openTelemetrySpanExporter.getKind();
  }

  @Override
  public <T> T getAttribute(AttributeKey<T> key) {
    throw new UnsupportedOperationException();
  }

  public OpenTelemetrySpanExporter getOpenTelemetrySpanExporter() {
    return openTelemetrySpanExporter;
  }
}
