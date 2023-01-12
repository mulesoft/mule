/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.ARTIFACT_ID;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.ARTIFACT_TYPE;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTIONS_HAVE_BEEN_RECORDED;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTION_ESCAPED_KEY;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTION_EVENT_NAME;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTION_MESSAGE_KEY;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTION_STACK_TRACE_KEY;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.EXCEPTION_TYPE_KEY;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.SPAN_KIND;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.STATUS;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.THREAD_END_NAME_KEY;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.getNameWithoutNamespace;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryTraceIdUtils.extractContextFromTraceParent;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryTraceIdUtils.generateSpanId;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryTraceIdUtils.generateTraceId;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.common.InstrumentationLibraryInfo.create;
import static io.opentelemetry.sdk.trace.data.StatusData.ok;
import static io.opentelemetry.api.trace.SpanContext.getInvalid;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter, SpanData, ReadableSpan, Attributes {

  private static final String MULE_INSTRUMENTATION_LIBRARY = "mule";
  private static final String MULE_INSTRUMENTATION_LIBRARY_VERSION = "1.0.0";
  // These are artifact.id, artifact.type and thread.end.name.
  public static final int EXPORTER_ATTRIBUTES_BASE_SIZE = 3;

  private final boolean isRootSpan;
  private final boolean isPolicySpan;
  private final InternalSpan internalSpan;
  private final String artifactId;
  private final String artifactType;
  private final Map<String, String> rootAttributes = new HashMap<>();
  private final SpanProcessor spanProcessor;

  private boolean exportable;
  private SpanContext spanContext;
  private SpanContext parentSpanContext = getInvalid();
  private SpanKind spanKind = INTERNAL;
  private StatusData statusData = ok();
  private List<EventData> errorEvents = emptyList();
  private String overriddenSpanName;
  private Set<String> noExportUntil;
  private OpenTelemetrySpanExporter rootSpan = this;
  private String rootName;
  private String endThreadNameValue;


  private OpenTelemetrySpanExporter(InternalSpan internalSpan,
                                    InitialSpanInfo initialSpanInfo,
                                    String artifactId,
                                    String artifactType,
                                    SpanProcessor spanProcessor) {
    this.internalSpan = internalSpan;
    this.noExportUntil = initialSpanInfo.getInitialExportInfo().noExportUntil();
    this.isRootSpan = initialSpanInfo.isRootSpan();
    this.isPolicySpan = initialSpanInfo.isPolicySpan();
    this.exportable = initialSpanInfo.getInitialExportInfo().isExportable();
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.spanProcessor = spanProcessor;

    // Generates the span id so that the opentelemetry spans can be lazily initialised.
    this.spanContext = SpanContext.create(generateTraceId(getAsInternalSpan(internalSpan.getParent())), generateSpanId(),
                                          TraceFlags.getSampled(), TraceState.getDefault());
  }

  @Nullable
  @Override
  public <T> T get(@Nullable AttributeKey<T> attributeKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEach(BiConsumer<? super AttributeKey<?>, ? super Object> biConsumer) {
    biConsumer.accept(ARTIFACT_ID, artifactId);
    biConsumer.accept(ARTIFACT_TYPE, artifactType);
    biConsumer.accept(THREAD_END_NAME_KEY, endThreadNameValue);
    internalSpan.forEachAttribute((key, value) -> {
      if (!key.equals(SPAN_KIND) && !key.equals(STATUS)) {
        biConsumer.accept(stringKey(key), value);
      }
    });
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

  public static OpenTelemetrySpanExportBuilder builder() {
    return new OpenTelemetrySpanExportBuilder();
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
    if (rootSpan != this) {
      rootSpan.updateNameForExport(newName);
    } else {
      overriddenSpanName = newName;
    }
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return OpenTelemetryTraceIdUtils.getContext(this);
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
    parentSpanContext = extractContextFromTraceParent(serializeAsMap.get("traceparent"));
  }

  @Override
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter childOpenTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;

      // If it isn't exportable propagate the traceId and spanId
      if (!childOpenTelemetrySpanExporter.exportable) {
        childOpenTelemetrySpanExporter.parentSpanContext = parentSpanContext;
        childOpenTelemetrySpanExporter.spanContext = spanContext;
        childOpenTelemetrySpanExporter.rootSpan = rootSpan;
        childOpenTelemetrySpanExporter.noExportUntil = noExportUntil;
        childOpenTelemetrySpanExporter.setRootName(rootName);
        return;
      }

      // If it is a policy span, propagate the rootSpan.
      if (childOpenTelemetrySpanExporter.isPolicySpan) {
        childOpenTelemetrySpanExporter.setRootName(rootName);
        childOpenTelemetrySpanExporter.rootSpan = rootSpan;
      }

      // Propagates the root name until it finds a root.
      if (rootName != null) {
        childOpenTelemetrySpanExporter.setRootName(rootName);
        rootAttributes.forEach(childOpenTelemetrySpanExporter::setRootAttribute);
      }

      // In case "no export until" is set, and it is not a child span that resets that condition (because
      // we have a span that begins again to be exportable), we have to propagate that condition to the
      // child span.
      if (!noExportUntil.isEmpty()
          && !noExportUntil.contains(getNameWithoutNamespace(childSpanExporter.getInternalSpan().getName()))) {
        childOpenTelemetrySpanExporter.parentSpanContext = parentSpanContext;
        childOpenTelemetrySpanExporter.noExportUntil = noExportUntil;
        childOpenTelemetrySpanExporter.spanContext = spanContext;
        childOpenTelemetrySpanExporter.rootSpan = rootSpan;
        childOpenTelemetrySpanExporter.exportable = false;
      }

      if (childOpenTelemetrySpanExporter.parentSpanContext == getInvalid()) {
        childOpenTelemetrySpanExporter.parentSpanContext = spanContext;
      }
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
      spanKind = SpanKind.valueOf(value);
    }

    if (key.equals(STATUS)) {
      StatusCode statusCode = StatusCode.valueOf(value);
      statusData = StatusData.create(statusCode, null);
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
    return create(MULE_INSTRUMENTATION_LIBRARY, MULE_INSTRUMENTATION_LIBRARY_VERSION);
  }

  @Override
  public Resource getResource() {
    return OpenTelemetryResources.getResource(artifactId);
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

  @Nullable
  @Override
  public <T> T getAttribute(@Nullable AttributeKey<T> attributeKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return SpanData.super.getInstrumentationScopeInfo();
  }

  /**
   * Builder
   */
  public static class OpenTelemetrySpanExportBuilder {

    private InitialSpanInfo initialSpanInfo;
    private InternalSpan internalSpan;
    private String artifactId;
    private String artifactType;
    private SpanProcessor spanProcessor;

    public OpenTelemetrySpanExportBuilder withStartSpanInfo(InitialSpanInfo initialSpanInfo) {
      this.initialSpanInfo = initialSpanInfo;
      return this;
    }

    public OpenTelemetrySpanExportBuilder withInternalSpan(InternalSpan internalSpan) {
      this.internalSpan = internalSpan;
      return this;
    }

    public OpenTelemetrySpanExportBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public OpenTelemetrySpanExportBuilder withArtifactType(String artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public OpenTelemetrySpanExportBuilder withSpanProcessor(SpanProcessor spanProcessor) {
      this.spanProcessor = spanProcessor;
      return this;
    }

    public OpenTelemetrySpanExporter build() {

      if (initialSpanInfo == null) {
        throw new IllegalArgumentException("Start span info is null");
      }

      if (artifactId == null) {
        throw new IllegalArgumentException("Artifact id is null");
      }

      if (artifactType == null) {
        throw new IllegalArgumentException("Artifact type is null");
      }

      if (spanProcessor == null) {
        throw new IllegalArgumentException("Artifact type is null");
      }

      return new OpenTelemetrySpanExporter(internalSpan, initialSpanInfo, artifactId, artifactType, spanProcessor);
    }
  }

}
