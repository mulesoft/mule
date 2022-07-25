/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan.getAsInternalSpan;

import static java.time.Instant.ofEpochMilli;

import static org.mule.runtime.core.internal.trace.DistributedTraceContext.emptyDistributedEventContext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanVisitor;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * A {@link InternalSpanExporter} that exports the {@link InternalSpan}'s as open telemetry spans.
 *
 * @since 4.5.0
 */
public class OpentelemetrySpanExporter implements InternalSpanExporter {

  private static final TextMapGetter<Map<String, String>> OPENTELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  private static final OpentelemetryParentSpanVisitor OPENTELEMETRY_PARENT_SPAN_VISITOR = new OpentelemetryParentSpanVisitor();

  public static final OpentelemetryParentSpanVisitor.OpentelemetrySpanVisitor OPENTELEMETRY_SPAN_VISITOR =
      new OpentelemetryParentSpanVisitor.OpentelemetrySpanVisitor();

  private final Tracer tracer;
  private final Context remoteContext;
  private final io.opentelemetry.api.trace.Span openTelemetrySpan;

  public OpentelemetrySpanExporter(Tracer tracer, EventContext eventContext,
                                   InternalSpan internalSpan) {
    this.tracer = tracer;
    remoteContext = resolveRemoteContext(eventContext);
    openTelemetrySpan = resolveOpentelemetrySpan(internalSpan);
  }

  @Override
  public void export(InternalSpan internalSpan) {
    openTelemetrySpan.end(ofEpochMilli(internalSpan.getDuration().getEnd()));
  }

  @Override
  public <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor) {
    return internalSpanExporterVisitor.accept(this);
  }

  private Context resolveParentOpentelemetrySpan(InternalSpan internalSpan) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    if (parentSpan == null) {
      return null;
    }

    io.opentelemetry.api.trace.Span parentOpentelemetrySpan = parentSpan.visit(OPENTELEMETRY_PARENT_SPAN_VISITOR);

    if (parentOpentelemetrySpan != null) {
      return Context.current().with(parentOpentelemetrySpan);
    }

    return remoteContext;
  }

  private Context resolveRemoteContext(EventContext eventContext) {
    DistributedTraceContext distributedTraceContext = resolveDistributedTraceContext(eventContext);

    return GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .extract(Context.current(), distributedTraceContext.tracingFieldsAsMap(), OPENTELEMETRY_SPAN_GETTER);
  }

  private DistributedTraceContext resolveDistributedTraceContext(EventContext eventContext) {
    if (eventContext instanceof DistributedTraceContextAware) {
      return ((DistributedTraceContextAware) eventContext).getDistributedTraceContext();
    }

    return emptyDistributedEventContext();
  }


  private io.opentelemetry.api.trace.Span resolveOpentelemetrySpan(InternalSpan internalSpan) {
    SpanBuilder spanBuilder = tracer.spanBuilder(internalSpan.getName());

    Context parentSpanContext = resolveParentOpentelemetrySpan(internalSpan);

    if (parentSpanContext != null) {
      spanBuilder = spanBuilder.setParent(parentSpanContext);
    }

    return spanBuilder.setStartTimestamp(ofEpochMilli(internalSpan.getDuration().getStart()))
        .startSpan();
  }

  public io.opentelemetry.api.trace.Span getOpentelemetrySpan() {
    return openTelemetrySpan;
  }

  /**
   * An Internal {@link TextMapGetter} to retrieve the remote span context.
   *
   * This is used to resolve a remote OpTel Span propagated through W3C Trace Context.
   */
  private static class MuleOpenTelemetryRemoteContextGetter implements TextMapGetter<Map<String, String>> {

    @Override
    public Iterable<String> keys(Map<String, String> stringStringMap) {
      return stringStringMap.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, String> stringStringMap, String s) {
      if (stringStringMap == null) {
        return null;
      }

      return stringStringMap.get(s);
    }
  }

  /**
   * A visitor to get the open telemetry span.
   */
  private static class OpentelemetryParentSpanVisitor implements InternalSpanVisitor<io.opentelemetry.api.trace.Span> {

    @Override
    public io.opentelemetry.api.trace.Span accept(ExportOnEndSpan exportOnEndSpan) {
      return exportOnEndSpan.getSpanExporter().visit(OPENTELEMETRY_SPAN_VISITOR);
    }

    @Override
    public Span accept(ExecutionSpan executionSpan) {
      return null;
    }

    @Override
    public Span accept(InternalSpan.SpanInternalWrapper spanInternalWrapper) {
      return null;
    }

    private static class OpentelemetrySpanVisitor implements InternalSpanExporterVisitor<io.opentelemetry.api.trace.Span> {

      @Override
      public io.opentelemetry.api.trace.Span accept(OpentelemetrySpanExporter opentelemetrySpanExporter) {
        return opentelemetrySpanExporter.getOpentelemetrySpan();
      }
    }
  }

}
