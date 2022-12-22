/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

import java.util.Queue;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.sdk.trace.internal.JcTools.newFixedSizeQueue;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.OPEN_TELEMETRY_SPAN_GETTER;
import static org.mule.runtime.tracer.impl.exporter.config.SpanExporterConfigurationDiscoverer.discoverSpanExporterConfiguration;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources.getPropagator;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.ThreadIdGenerator.setSpanIdHolder;

public class OpentelemetryExportQueue {

  private static final SpanExporterConfiguration CONFIGURATION = discoverSpanExporterConfiguration();

  public static final String SPAN_KIND = "span.kind.override";
  public static final String STATUS = "status.override";

  private final Queue<OpenTelemetrySpanExporter> queue;

  public OpentelemetryExportQueue() {
    queue = newFixedSizeQueue(1000);
    Thread workerThread = new DaemonThreadFactory("enqueuer").newThread(new Worker());
    workerThread.start();
  }

  public void offer(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
    queue.offer(openTelemetrySpanExporter);
  }

  private class Worker implements Runnable {

    public static final String EXCEPTIONS_HAS_BEEN_RECORDED = "Exceptions has been recorded.";

    public final String EXCEPTION_EVENT_NAME = "exception";
    public final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
    public final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
    public final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
    public final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");
    public static final String ARTIFACT_ID = "artifact.id";
    public static final String ARTIFACT_TYPE = "artifact.type";

    @Override
    public void run() {
      while (true) {
        if (queue.isEmpty()) {
          try {
            Thread.sleep(1000);
            continue;
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        OpenTelemetrySpanExporter spanExporter = queue.poll();
        InternalSpan internalSpan = spanExporter.getInternalSpan();

        String name = spanExporter.getName();

        if (name == null) {
          name = internalSpan.getName();
        }

        SpanBuilder spanBuilder =
            OpenTelemetryResources.getTracer(CONFIGURATION, spanExporter.getArtifactId()).spanBuilder(name);

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

        // override the span kind
        String spanKind = internalSpan.getAttributes().remove(SPAN_KIND);

        if (spanKind != null) {
          spanBuilder.setSpanKind(SpanKind.valueOf(spanKind));
        }


        setSpanIdHolder(spanExporter.getSpanId());
        Span span = spanBuilder.setStartTimestamp(internalSpan.getDuration().getStart(), NANOSECONDS).startSpan();

        // override the span
        String spanStatus = internalSpan.getAttributes().remove(STATUS);
        if (spanStatus != null) {
          span.setStatus(StatusCode.valueOf(spanStatus));
        }

        if (internalSpan.hasErrors()) {
          span.setStatus(ERROR, EXCEPTIONS_HAS_BEEN_RECORDED);
          recordSpanExceptions(internalSpan, span);
        }

        span.setAttribute(ARTIFACT_ID, spanExporter.getArtifactId());
        span.setAttribute(ARTIFACT_TYPE, spanExporter.getArtifactType());

        internalSpan.getAttributes().forEach(span::setAttribute);

        span.end(internalSpan.getDuration().getEnd(), NANOSECONDS);
      }
    }

    private void recordSpanExceptions(InternalSpan internalSpan, Span span) {
      internalSpan.getErrors().forEach(error -> recordSpanException(error, span));
    }

    private void recordSpanException(SpanError spanError, Span span) {
      Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                      EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                      EXCEPTION_STACK_TRACE_KEY,
                                      getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                      EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());
      span.addEvent(EXCEPTION_EVENT_NAME, errorAttributes);
    }
  }
}
