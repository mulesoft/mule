/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.api.profiling.tracing.SpanIdentifier.INVALID_SPAN_IDENTIFIER;
import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.exporter.impl.MutableMuleTraceState.getMutableMuleTraceStateFrom;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTIONS_HAVE_BEEN_RECORDED;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_ESCAPED_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_EVENT_NAME;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_MESSAGE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_STACK_TRACE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_TYPE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.SPAN_KIND;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.STATUS;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.extractContextFromTraceParent;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.SpanContext.getInvalid;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.sdk.trace.data.StatusData.unset;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.SpanAttribute;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.impl.optel.sdk.MuleReadableSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.StatusData;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter {

  public static final String TRACE_PARENT_KEY = "traceparent";
  private final boolean isRootSpan;
  private final boolean isPolicySpan;
  private final InternalSpan internalSpan;
  private final List<SpanAttribute<String>> rootAttributes = new ArrayList<>();
  private final SpanProcessor spanProcessor;
  private final boolean enableMuleAncestorIdManagement;
  private final InitialExportInfo initialExportInfo;
  private LazyValue<SpanContext> spanContext = new LazyValue<>(this::createSpanContext);
  private SpanContext parentSpanContext = getInvalid();
  private SpanKind spanKind = INTERNAL;
  private StatusData statusData = unset();
  private List<EventData> errorEvents = emptyList();
  private String overriddenSpanName;
  private OpenTelemetrySpanExporter rootSpanExporter = this;
  private String rootName;
  private String endThreadNameValue;
  private MutableMuleTraceState muleTraceState;
  private final ReadableSpan readableSpan;

  public OpenTelemetrySpanExporter(InternalSpan internalSpan,
                                   InitialSpanInfo initialSpanInfo,
                                   String artifactId,
                                   String artifactType,
                                   SpanProcessor spanProcessor,
                                   boolean enableMuleAncestorIdManagement,
                                   Resource resource) {
    requireNonNull(internalSpan);
    requireNonNull(initialSpanInfo);
    requireNonNull(artifactId);
    requireNonNull(artifactType);
    requireNonNull(spanProcessor);
    requireNonNull(resource);
    this.internalSpan = internalSpan;
    this.isRootSpan = initialSpanInfo.isRootSpan();
    this.isPolicySpan = initialSpanInfo.isPolicySpan();
    this.spanProcessor = spanProcessor;
    this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
    this.muleTraceState = getMutableMuleTraceStateFrom(emptyMap(), enableMuleAncestorIdManagement);
    this.initialExportInfo = initialSpanInfo.getInitialExportInfo();
    this.readableSpan = new MuleReadableSpan(this, resource, artifactId, artifactType);
  }

  @Override
  public void export() {
    if (initialExportInfo.isExportable()) {
      endThreadNameValue = Thread.currentThread().getName();
      spanProcessor.onEnd(readableSpan);
    }
  }

  @Override
  public void updateNameForExport(String newName) {
    if (rootSpanExporter != this) {
      rootSpanExporter.updateNameForExport(newName);
    } else {
      overriddenSpanName = newName;
    }
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return OpenTelemetryTraceIdUtils.getDistributedTraceContext(this, enableMuleAncestorIdManagement);
  }

  @Override
  public void setRootName(String rootName) {
    if (isRootSpan) {
      overriddenSpanName = rootName;
    } else {
      this.rootName = rootName;
    }
  }

  @Override
  public void setRootAttribute(SpanAttribute<String> spanAttribute) {
    if (isRootSpan) {
      internalSpan.addAttribute(spanAttribute);
    } else {
      this.rootAttributes.add(spanAttribute);
    }
  }

  @Override
  public void updateParentSpanFrom(Map<String, String> serializeAsMap) {
    parentSpanContext = extractContextFromTraceParent(serializeAsMap.get(TRACE_PARENT_KEY));
    muleTraceState = getMutableMuleTraceStateFrom(serializeAsMap, enableMuleAncestorIdManagement);
  }

  private SpanContext createSpanContext() {
    // Generates the span id so that the OpenTelemetry spans can be lazily initialised if it is exportable
    if (initialExportInfo.isExportable()) {
      String spanId = OpenTelemetryTraceIdUtils.generateSpanId();
      String traceId = parentSpanContext.isValid() ? parentSpanContext.getTraceId()
          : OpenTelemetryTraceIdUtils.generateTraceId(getAsInternalSpan(internalSpan.getParent()));
      return SpanContext.create(traceId, spanId, TraceFlags.getSampled(), muleTraceState);
    } else {
      return parentSpanContext;
    }
  }

  @Override
  public SpanIdentifier getSpanIdentifier() {
    if (spanContext.get().isValid()) {
      return new OpentelemetrySpanIdentifier(spanContext.get().getSpanId(), spanContext.get().getTraceId());
    } else {
      return INVALID_SPAN_IDENTIFIER;
    }
  }

  @Override
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter childOpenTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;

      muleTraceState.propagateRemoteContext(childOpenTelemetrySpanExporter.muleTraceState);

      childOpenTelemetrySpanExporter.initialExportInfo.propagateInitialExportInfo(this.initialExportInfo);

      // Propagates the root name until it finds a root.
      if (rootName != null) {
        childOpenTelemetrySpanExporter.setRootName(rootName);
        rootAttributes.forEach(childOpenTelemetrySpanExporter::setRootAttribute);
      }

      // If it isn't exportable propagate the traceId and spanId
      if (!childOpenTelemetrySpanExporter.initialExportInfo.isExportable()) {
        childOpenTelemetrySpanExporter.parentSpanContext = parentSpanContext;
        childOpenTelemetrySpanExporter.spanContext = spanContext;
        childOpenTelemetrySpanExporter.rootSpanExporter = rootSpanExporter;
        return;
      }
      childOpenTelemetrySpanExporter.parentSpanContext = spanContext.get();

      // If it is a policy span, propagate the rootSpan.
      if (childOpenTelemetrySpanExporter.isPolicySpan) {
        childOpenTelemetrySpanExporter.rootSpanExporter = rootSpanExporter;
      }
    }
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }

  public String getName() {
    if (overriddenSpanName == null) {
      return internalSpan.getName();
    }

    return overriddenSpanName;
  }

  @Override
  public void onAdditionalAttribute(SpanAttribute<String> spanAttribute) {
    if (spanAttribute.getKey().equals(SPAN_KIND)) {
      rootSpanExporter.spanKind = SpanKind.valueOf(spanAttribute.getValue());
    } else if (spanAttribute.getKey().equals(STATUS)) {
      StatusCode statusCode = StatusCode.valueOf(spanAttribute.getValue());
      rootSpanExporter.statusData = StatusData.create(statusCode, null);
    } else if (isPolicySpan && !rootSpanExporter.equals(this)) {
      rootSpanExporter.internalSpan.addAttribute(spanAttribute);
    }
  }

  public SpanContext getSpanContext() {
    return spanContext.get();
  }

  public SpanContext getParentSpanContext() {
    return parentSpanContext;
  }


  public StatusData getStatus() {
    return statusData;
  }

  public List<EventData> getEvents() {
    return errorEvents;
  }

  @Override
  public void onError(InternalSpanError spanError) {
    statusData = StatusData.create(ERROR, EXCEPTIONS_HAVE_BEEN_RECORDED);
    Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                    EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                    EXCEPTION_STACK_TRACE_KEY,
                                    getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                    EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());

    errorEvents = singletonList(EventData.create(System.currentTimeMillis(), EXCEPTION_EVENT_NAME, errorAttributes));
  }

  public MutableMuleTraceState getTraceState() {
    return muleTraceState;
  }

  public SpanKind getKind() {
    return spanKind;
  }

  public String getThreadEndName() {
    return endThreadNameValue;
  }

  public String getSpanId() {
    return spanContext.get().getSpanId();
  }

  public String getTraceId() {
    return spanContext.get().getTraceId();
  }

}
