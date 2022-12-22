/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.OPEN_TELEMETRY_SPAN_GETTER;
import static org.mule.runtime.tracer.impl.exporter.config.SpanExporterConfigurationDiscoverer.discoverSpanExporterConfiguration;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources.getPropagator;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources.getTracer;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.ThreadLocalIdGenerator.setOpenTelemetrySpanId;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

/**
 * Utils for exporting Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterUtils {

  private OpenTelemetrySpanExporterUtils() {}

  private static final SpanExporterConfiguration CONFIGURATION = discoverSpanExporterConfiguration();

  public static final String EXCEPTION_EVENT_NAME = "exception";
  public static final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
  public static final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
  public static final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
  public static final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");
  public static final String EXCEPTIONS_HAS_BEEN_RECORDED = "Exceptions have been recorded.";

  public static final String ARTIFACT_ID = "artifact.id";
  public static final String ARTIFACT_TYPE = "artifact.type";

  public static final String SPAN_KIND = "span.kind.override";
  public static final String STATUS = "status.override";

  public static void processSpanExporter(OpenTelemetrySpanExporter spanExporter) {
    InternalSpan internalSpan = spanExporter.getInternalSpan();

    // Resolve span name
    String name = resolveSpanName(spanExporter, internalSpan);

    // Get Open Telemetry Span Builder.
    SpanBuilder spanBuilder = getTracer(CONFIGURATION, spanExporter.getArtifactId()).spanBuilder(name);

    // Process parent Span
    resolveParentSpan(internalSpan, spanBuilder);

    // Override the span kind
    processSpanKind(internalSpan, spanBuilder);

    // Sets the span id.
    setOpenTelemetrySpanId(spanExporter.getSpanId());

    // Starts the span the with corresponding timestamp.
    Span span = spanBuilder.setStartTimestamp(internalSpan.getDuration().getStart(), NANOSECONDS).startSpan();

    // Override the span status.
    processSpanStatus(internalSpan, span);

    // Process errors
    processErrors(internalSpan, span);

    // Adds the span attributes
    addAttributes(span, internalSpan, spanExporter);

    // Ends the span with the corresponding timestamp
    span.end(internalSpan.getDuration().getEnd(), NANOSECONDS);
  }

  public static void processSpanKind(InternalSpan internalSpan, SpanBuilder spanBuilder) {
    String spanKind = internalSpan.getAttributes().remove(SPAN_KIND);

    if (spanKind != null) {
      spanBuilder.setSpanKind(SpanKind.valueOf(spanKind));
    }
  }

  public static void processSpanStatus(InternalSpan internalSpan, Span span) {
    String spanStatus = internalSpan.getAttributes().remove(STATUS);
    if (spanStatus != null) {
      span.setStatus(StatusCode.valueOf(spanStatus));
    }

  }

  public static void resolveParentSpan(InternalSpan internalSpan, SpanBuilder spanBuilder) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    // Configure the parent span.
    if (parentSpan != null) {
      SpanExporter parentSpanExporter = parentSpan.getSpanExporter();

      if (parentSpanExporter instanceof OpenTelemetrySpanExporter) {
        spanBuilder.setParent(((OpenTelemetrySpanExporter) parentSpanExporter).getOpenTelemetrySpanContext());
      } else {
        spanBuilder.setParent(getPropagator().getTextMapPropagator().extract(Context.current(), parentSpan.serializeAsMap(),
                                                                             OPEN_TELEMETRY_SPAN_GETTER));
      }
    }
  }

  public static String resolveSpanName(OpenTelemetrySpanExporter spanExporter, InternalSpan internalSpan) {
    String name = spanExporter.getOverridenSpanName();

    if (name == null) {
      name = internalSpan.getName();
    }

    return name;
  }

  public static String getNameWithoutNamespace(String name) {
    int index = name.lastIndexOf(":");
    if (index != -1) {
      return name.substring(index + 1);
    } else {
      return name;
    }
  }

  public static void processErrors(InternalSpan internalSpan, Span span) {
    if (internalSpan.hasErrors()) {
      span.setStatus(ERROR, EXCEPTIONS_HAS_BEEN_RECORDED);
      OpenTelemetrySpanExporterUtils.recordSpanExceptions(internalSpan, span);
    }
  }

  public static void addAttributes(Span span, InternalSpan internalSpan, OpenTelemetrySpanExporter spanExporter) {
    span.setAttribute(ARTIFACT_ID, spanExporter.getArtifactId());
    span.setAttribute(ARTIFACT_TYPE, spanExporter.getArtifactType());

    internalSpan.getAttributes().forEach(span::setAttribute);

    span.end(internalSpan.getDuration().getEnd(), NANOSECONDS);

    internalSpan.getAttributes().forEach(span::setAttribute);
  }

  private static void recordSpanExceptions(InternalSpan internalSpan, Span span) {
    internalSpan.getErrors().forEach(error -> recordSpanException(error, span));
  }

  private static void recordSpanException(SpanError spanError, Span span) {
    Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                    EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                    EXCEPTION_STACK_TRACE_KEY,
                                    getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                    EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());
    span.addEvent(EXCEPTION_EVENT_NAME, errorAttributes);
  }
}
