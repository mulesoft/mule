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
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.ARTIFACT_ID;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.ARTIFACT_TYPE;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTIONS_HAVE_BEEN_RECORDED;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_ESCAPED_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_EVENT_NAME;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_MESSAGE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_STACK_TRACE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.EXCEPTION_TYPE_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.SPAN_KIND;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.STATUS;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.THREAD_END_NAME_KEY;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.getNameWithoutNamespace;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetryTraceIdUtils.extractContextFromTraceParent;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.SpanContext.getInvalid;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.sdk.common.InstrumentationLibraryInfo.create;
import static io.opentelemetry.sdk.internal.InstrumentationScopeUtil.toInstrumentationScopeInfo;
import static io.opentelemetry.sdk.trace.data.StatusData.unset;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter, SpanData, ReadableSpan, Attributes {

  private static final String MULE_INSTRUMENTATION_LIBRARY = "mule";
  private static final String MULE_INSTRUMENTATION_LIBRARY_VERSION = "1.0.0";
  public static final InstrumentationLibraryInfo INSTRUMENTATION_LIBRARY_INFO =
      create(MULE_INSTRUMENTATION_LIBRARY, MULE_INSTRUMENTATION_LIBRARY_VERSION);
  public static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO =
      toInstrumentationScopeInfo(INSTRUMENTATION_LIBRARY_INFO);
  // These are artifact.id, artifact.type and thread.end.name.
  public static final int EXPORTER_ATTRIBUTES_BASE_SIZE = 3;
  public static final String TRACE_PARENT_KEY = "traceparent";
  private final boolean isRootSpan;
  private final boolean isPolicySpan;
  private final InternalSpan internalSpan;
  private final String artifactId;
  private final String artifactType;
  private final Map<String, String> rootAttributes = new HashMap<>();
  private final SpanProcessor spanProcessor;
  private final Resource resource;
  private final boolean enableMuleAncestorIdManagement;
  private final InitialExportInfo initialExportInfo;

  private boolean exportable;
  private SpanContext spanContext = getInvalid();
  private SpanContext parentSpanContext = getInvalid();
  private SpanKind spanKind = INTERNAL;
  private StatusData statusData = unset();
  private List<EventData> errorEvents = emptyList();
  private String overriddenSpanName;
  private Set<String> noExportUntil;
  private OpenTelemetrySpanExporter rootSpanExporter = this;
  private String rootName;
  private String endThreadNameValue;

  private MutableMuleTraceState muleTraceState;

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
    this.noExportUntil = initialSpanInfo.getInitialExportInfo().noExportUntil();
    this.isRootSpan = initialSpanInfo.isRootSpan();
    this.isPolicySpan = initialSpanInfo.isPolicySpan();
    this.exportable = initialSpanInfo.getInitialExportInfo().isExportable();
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.spanProcessor = spanProcessor;
    this.enableMuleAncestorIdManagement = enableMuleAncestorIdManagement;
    this.resource = resource;
    this.muleTraceState = getMutableMuleTraceStateFrom(emptyMap(), enableMuleAncestorIdManagement);
    this.initialExportInfo = initialSpanInfo.getInitialExportInfo();
    createSpanContext();
  }

  @Override
  public <T> T get(AttributeKey<T> attributeKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEach(BiConsumer<? super AttributeKey<?>, ? super Object> biConsumer) {
    biConsumer.accept(ARTIFACT_ID, artifactId);
    biConsumer.accept(ARTIFACT_TYPE, artifactType);
    biConsumer.accept(THREAD_END_NAME_KEY, endThreadNameValue);
    internalSpan.forEachAttribute((key, value) -> biConsumer.accept(stringKey(key), value));

  }

  @Override
  public int size() {
    return EXPORTER_ATTRIBUTES_BASE_SIZE + internalSpan.getAttributesCount();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Map<AttributeKey<?>, Object> asMap() {
    Map<AttributeKey<?>, Object> attributes = new HashMap<>();
    forEach(attributes::put);
    return attributes;
  }

  @Override
  public AttributesBuilder toBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void export() {
    if (exportable) {
      endThreadNameValue = Thread.currentThread().getName();
      spanProcessor.onEnd(this);
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
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    if (isRootSpan) {
      internalSpan.addAttribute(rootAttributeKey, rootAttributeValue);
    } else {
      this.rootAttributes.put(rootAttributeKey, rootAttributeValue);
    }
  }

  @Override
  public void updateParentSpanFrom(Map<String, String> serializeAsMap) {
    parentSpanContext = extractContextFromTraceParent(serializeAsMap.get(TRACE_PARENT_KEY));
    muleTraceState = getMutableMuleTraceStateFrom(serializeAsMap, enableMuleAncestorIdManagement);
    if (parentSpanContext.isValid()) {
      if (!exportable) {
        // if it not exportable, we set the parent span context so that it is eventually
        // propagated to the next exportable span.
        spanContext = SpanContext.create(parentSpanContext.getTraceId(), parentSpanContext.getSpanId(),
                                         TraceFlags.getSampled(), muleTraceState);
      } else {
        spanContext = SpanContext.create(parentSpanContext.getTraceId(), spanContext.getSpanId(),
                                         TraceFlags.getSampled(), muleTraceState);
      }
    }
  }

  @Override
  public SpanIdentifier getSpanIdentifier() {
    if (spanContext.isValid()) {
      return new OpentelemetrySpanIdentifier(spanContext.getSpanId(), spanContext.getTraceId());
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
      childOpenTelemetrySpanExporter.update();

      // In case "no export until" is set, and it is not a child span that resets that condition (because
      // we have a span that begins again to be exportable), we have to propagate that condition to the
      // child span.
      if (!noExportUntil.isEmpty()
          && !noExportUntil.contains(getNameWithoutNamespace(childSpanExporter.getInternalSpan().getName()))) {
        childOpenTelemetrySpanExporter.exportable = false;
        childOpenTelemetrySpanExporter.noExportUntil = noExportUntil;
      }

      // Propagates the root name until it finds a root.
      if (rootName != null) {
        childOpenTelemetrySpanExporter.setRootName(rootName);
        rootAttributes.forEach(childOpenTelemetrySpanExporter::setRootAttribute);
      }

      // If it isn't exportable propagate the traceId and spanId
      if (!childOpenTelemetrySpanExporter.exportable) {
        childOpenTelemetrySpanExporter.parentSpanContext = parentSpanContext;
        childOpenTelemetrySpanExporter.spanContext = spanContext;
        childOpenTelemetrySpanExporter.rootSpanExporter = rootSpanExporter;
        return;
      }

      // If it is a policy span, propagate the rootSpan.
      if (childOpenTelemetrySpanExporter.isPolicySpan) {
        childOpenTelemetrySpanExporter.rootSpanExporter = rootSpanExporter;
      }

      if (childOpenTelemetrySpanExporter.parentSpanContext == getInvalid()) {
        childOpenTelemetrySpanExporter.parentSpanContext = spanContext;
      }
    }
  }

  private void update() {
    this.exportable = this.initialExportInfo.isExportable();
    this.noExportUntil = this.initialExportInfo.noExportUntil();
    createSpanContext();
  }

  private void createSpanContext() {
    // Generates the span id so that the OpenTelemetry spans can be lazily initialised if it is exportable
    if (this.exportable && !this.spanContext.isValid()) {
      String spanId = OpenTelemetryTraceIdUtils.generateSpanId();
      String traceId = OpenTelemetryTraceIdUtils.generateTraceId(getAsInternalSpan(internalSpan.getParent()));
      this.spanContext = SpanContext.create(traceId, spanId, TraceFlags.getSampled(), muleTraceState);
    }
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }

  @Override
  public String getName() {
    if (overriddenSpanName == null) {
      return internalSpan.getName();
    }

    return overriddenSpanName;
  }

  @Override
  public SpanKind getKind() {
    return spanKind;
  }

  @Override
  public void onAdditionalAttribute(String key, String value) {
    if (key.equals(SPAN_KIND)) {
      rootSpanExporter.spanKind = SpanKind.valueOf(value);
    } else if (key.equals(STATUS)) {
      StatusCode statusCode = StatusCode.valueOf(value);
      rootSpanExporter.statusData = StatusData.create(statusCode, null);
    } else if (isPolicySpan && !rootSpanExporter.equals(this)) {
      rootSpanExporter.internalSpan.addAttribute(key, value);
    }
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public SpanContext getParentSpanContext() {
    return parentSpanContext;
  }

  @Override
  public String getParentSpanId() {
    return SpanData.super.getParentSpanId();
  }

  @Override
  public StatusData getStatus() {
    return statusData;
  }

  @Override
  public long getStartEpochNanos() {
    return internalSpan.getDuration().getStart();
  }

  @Override
  public Attributes getAttributes() {
    return this;
  }

  @Override
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

  @Override
  public List<LinkData> getLinks() {
    return emptyList();
  }

  @Override
  public long getEndEpochNanos() {
    return internalSpan.getDuration().getEnd();
  }

  @Override
  public boolean hasEnded() {
    return true;
  }

  @Override
  public int getTotalRecordedEvents() {
    // This is for performance purposes. We know that in the current
    // implementation we only have one error. So we inform this to the open
    // telemetry sdk.
    if (errorEvents.isEmpty()) {
      return 0;
    }

    return 1;
  }

  @Override
  public int getTotalRecordedLinks() {
    return 0;
  }

  @Override
  public int getTotalAttributeCount() {
    return size();
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return INSTRUMENTATION_LIBRARY_INFO;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public String getOverriddenSpanName() {
    return overriddenSpanName;
  }

  @Override
  public SpanData toSpanData() {
    return this;
  }

  @Override
  public long getLatencyNanos() {
    return 0;
  }

  @Override
  public <T> T getAttribute(AttributeKey<T> attributeKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return INSTRUMENTATION_SCOPE_INFO;
  }

  public MutableMuleTraceState getTraceState() {
    return muleTraceState;
  }

}
