/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getComponentNameWithoutNamespace;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.getPropagator;
import static org.mule.runtime.core.internal.profiling.tracing.export.NoExportableOpenTelemetrySpan.getNoExportableOpentelemetrySpan;
import static org.mule.runtime.core.internal.trace.DistributedTraceContext.emptyDistributedEventContext;

import static java.util.Collections.emptySet;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanError;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanVisitor;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * A {@link InternalSpanExporter} that exports the {@link InternalSpan}'s as Open Telemetry spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements InternalSpanExporter {

  private static final TextMapGetter<Map<String, String>> OPEN_TELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  private static final OpenTelemetryParentSpanVisitor OPEN_TELEMETRY_PARENT_SPAN_VISITOR = new OpenTelemetryParentSpanVisitor();

  public static final OpenTelemetryParentSpanVisitor.OpenTelemetrySpanVisitor OPEN_TELEMETRY_SPAN_VISITOR =
      new OpenTelemetryParentSpanVisitor.OpenTelemetrySpanVisitor();


  // TODO: W-11610439: Track if the semconv dependency (alpha) should be added.
  public static final String EXCEPTION_EVENT_NAME = "exception";
  public static final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
  public static final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
  public static final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
  public static final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");

  private final Tracer tracer;
  private final Context remoteContext;
  private io.opentelemetry.api.trace.Span openTelemetrySpan;
  private final boolean exportable;
  private Set<String> noExportUntil;
  private final InternalSpan internalSpan;
  private final Map<String, String> attributes = new HashMap<>();

  public OpenTelemetrySpanExporter(Tracer tracer, EventContext eventContext,
                                   boolean exportable,
                                   Set<String> noExportUntil,
                                   InternalSpan internalSpan) {
    this.tracer = tracer;
    this.internalSpan = internalSpan;
    this.noExportUntil = noExportUntil;
    remoteContext = resolveRemoteContext(eventContext);
    this.exportable = exportable;
  }

  private Set<String> resolveNoExportUntil(Set<String> noExportUntil, Set<String> parentNoExportUntil) {
    Set<String> finalNoExportUntil = new HashSet<>(noExportUntil);
    finalNoExportUntil.addAll(parentNoExportUntil);
    return finalNoExportUntil;
  }

  private Set<String> resolveParentNoExportUntil(InternalSpan internalSpan) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    if (parentSpan == null) {
      return emptySet();
    }

    while (parentSpan != null) {
      if (parentSpan instanceof ExportOnEndSpan) {
        return ((ExportOnEndSpan) parentSpan).getSpanExporter().noExportUntil();
      }
      parentSpan = getAsInternalSpan(parentSpan.getParent());
    }

    return emptySet();
  }

  @Override
  public void export(InternalSpan internalSpan) {
    if (internalSpan.hasErrors()) {
      recordSpanExceptions(internalSpan);
    }
    getOpenTelemetrySpan().end(internalSpan.getDuration().getEnd(), NANOSECONDS);
  }

  @Override
  public <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor) {
    return internalSpanExporterVisitor.accept(this);
  }

  @Override
  public Set<String> noExportUntil() {
    return noExportUntil;
  }

  private void recordSpanExceptions(InternalSpan internalSpan) {
    internalSpan.getErrors().forEach(this::recordSpanException);
  }

  private void recordSpanException(SpanError spanError) {
    Attributes errorAttributes = Attributes.of(
                                               EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                               EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                               EXCEPTION_STACK_TRACE_KEY,
                                               InternalSpanError.getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                               EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());
    getOpenTelemetrySpan().addEvent(EXCEPTION_EVENT_NAME, errorAttributes);
  }

  private Context resolveParentOpenTelemetrySpan(InternalSpan internalSpan) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    if (parentSpan == null) {
      return remoteContext;
    }

    io.opentelemetry.api.trace.Span parentOpenTelemetrySpan = null;

    while (parentOpenTelemetrySpan == null && parentSpan != null) {
      parentOpenTelemetrySpan = parentSpan.visit(OPEN_TELEMETRY_PARENT_SPAN_VISITOR);

      if (parentOpenTelemetrySpan == null) {
        break;
      }

      if (parentOpenTelemetrySpan.equals(getNoExportableOpentelemetrySpan())) {
        parentOpenTelemetrySpan = null;
        parentSpan = getAsInternalSpan(parentSpan.getParent());
      }
    }

    if (parentOpenTelemetrySpan != null) {
      return Context.current().with(parentOpenTelemetrySpan);
    }

    return remoteContext;
  }

  private Context resolveRemoteContext(EventContext eventContext) {
    DistributedTraceContext distributedTraceContext = resolveDistributedTraceContext(eventContext);

    return getPropagator().getTextMapPropagator()
        .extract(Context.current(), distributedTraceContext.tracingFieldsAsMap(), OPEN_TELEMETRY_SPAN_GETTER);
  }

  private DistributedTraceContext resolveDistributedTraceContext(EventContext eventContext) {
    if (eventContext instanceof DistributedTraceContextAware) {
      return ((DistributedTraceContextAware) eventContext).getDistributedTraceContext();
    }

    return emptyDistributedEventContext();
  }

  private io.opentelemetry.api.trace.Span resolveOpenTelemetrySpan(InternalSpan internalSpan, Set<String> parentNoExportUntil,
                                                                   Set<String> noExportUntil) {
    boolean exportableAccordingToName = exportableAccordingToName(internalSpan, parentNoExportUntil);

    // In case the hierarchy is exportable according to parent
    if (exportableAccordingToName) {
      this.noExportUntil = noExportUntil;
    } else {
      // Otherwise I have to merge when they begin to export.
      this.noExportUntil = resolveNoExportUntil(noExportUntil, parentNoExportUntil);
    }

    if ((!parentNoExportUntil.isEmpty() && !exportableAccordingToName) || !exportable) {
      return getNoExportableOpentelemetrySpan();
    }

    SpanBuilder spanBuilder = tracer.spanBuilder(internalSpan.getName());

    Context parentSpanContext = resolveParentOpenTelemetrySpan(internalSpan);

    if (parentSpanContext != null) {
      spanBuilder = spanBuilder.setParent(parentSpanContext);
    }

    io.opentelemetry.api.trace.Span span = spanBuilder.setStartTimestamp(internalSpan.getDuration().getStart(), NANOSECONDS)
        .startSpan();
    attributes.forEach(span::setAttribute);

    return span;
  }

  private boolean exportableAccordingToName(InternalSpan internalSpan, Set<String> parentNoExportUntil) {
    return parentNoExportUntil.contains(getComponentNameWithoutNamespace(internalSpan));
  }

  public io.opentelemetry.api.trace.Span getOpenTelemetrySpan() {
    if (openTelemetrySpan == null) {
      Set<String> parentNoExportUntil = resolveParentNoExportUntil(internalSpan);
      openTelemetrySpan = resolveOpenTelemetrySpan(internalSpan, parentNoExportUntil, noExportUntil);
    }

    return openTelemetrySpan;
  }

  public void setAttribute(String key, String value) {
    if (openTelemetrySpan == null) {
      this.attributes.put(key, value);
    } else {
      openTelemetrySpan.setAttribute(key, value);
    }
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
    public String get(@Nullable Map<String, String> stringStringMap, @Nullable String key) {
      if (stringStringMap == null) {
        return null;
      }

      return stringStringMap.get(key);
    }
  }

  /**
   * A visitor to get the open telemetry span.
   */
  private static class OpenTelemetryParentSpanVisitor implements InternalSpanVisitor<io.opentelemetry.api.trace.Span> {

    @Override
    public io.opentelemetry.api.trace.Span accept(ExportOnEndSpan exportOnEndSpan) {
      return exportOnEndSpan.getSpanExporter().visit(OPEN_TELEMETRY_SPAN_VISITOR);
    }

    @Override
    public Span accept(ExecutionSpan executionSpan) {
      return null;
    }

    @Override
    public Span accept(InternalSpan.SpanInternalWrapper spanInternalWrapper) {
      return null;
    }

    private static class OpenTelemetrySpanVisitor implements InternalSpanExporterVisitor<io.opentelemetry.api.trace.Span> {

      @Override
      public Span accept(OpenTelemetrySpanExporter opentelemetrySpanExporter) {
        return opentelemetrySpanExporter.getOpenTelemetrySpan();
      }
    }
  }
}
