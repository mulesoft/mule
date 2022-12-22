/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.getWithoutNamespace;
import static org.mule.runtime.tracer.impl.exporter.OpentelemetryTraceIdUtils.generateSpanId;
import static org.mule.runtime.tracer.impl.exporter.OpentelemetryTraceIdUtils.generateTraceId;
import static org.mule.runtime.tracer.impl.exporter.OpentelemetryTraceIdUtils.getContext;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * A {@link SpanExporter} that exports the spans as Open Telemetry Spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporter implements SpanExporter {

  private final boolean isRootSpan;
  private final boolean isPolicySpan;
  private final OpentelemetryExportQueue queue;
  private boolean exportable;

  public static final TextMapGetter<Map<String, String>> OPEN_TELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  public static OpenTelemetrySpanExportBuilder builder() {
    return new OpenTelemetrySpanExportBuilder();
  }

  private String name;
  private final InternalSpan internalSpan;

  private Set<String> noExportUntil;
  private final String artifactId;
  private final String artifactType;

  private final Map<String, String> rootAttributes = new HashMap<>();
  private OpenTelemetrySpanExporter rootSpan = this;
  private String rootName;
  private String spanId;
  private String traceId;

  private OpenTelemetrySpanExporter(InternalSpan internalSpan,
                                    InitialSpanInfo initialSpanInfo,
                                    String artifactId,
                                    String artifactType,
                                    OpentelemetryExportQueue queue) {
    this.internalSpan = internalSpan;
    this.noExportUntil = initialSpanInfo.getInitialExportInfo().noExportUntil();
    this.isRootSpan = initialSpanInfo.isRootSpan();
    this.isPolicySpan = initialSpanInfo.isPolicySpan();
    this.exportable = initialSpanInfo.getInitialExportInfo().isExportable();
    this.artifactId = artifactId;
    this.artifactType = artifactType;

    // Generates the span id so that the opentelemetry spans can be lazily initialised.
    this.spanId = generateSpanId();
    this.traceId = generateTraceId(getAsInternalSpan(internalSpan.getParent()));

    this.queue = queue;
  }

  @Override
  public void export() {
    if (exportable) {
      queue.offer(this);
    }
  }

  @Override
  public void updateNameForExport(String newName) {
    if (rootSpan != this) {
      rootSpan.updateNameForExport(newName);
    } else {
      name = newName;
    }
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return OpentelemetryTraceIdUtils.getContext(this);
  }

  @Override
  public void setRootName(String rootName) {
    if (isRootSpan) {
      name = rootName;
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
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter childOpenTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;

      // If it isn't exportable propagate the traceId and spanId
      if (!childOpenTelemetrySpanExporter.exportable) {
        childOpenTelemetrySpanExporter.traceId = traceId;
        childOpenTelemetrySpanExporter.spanId = spanId;
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

      // No export if no export till reset.
      if (!noExportUntil.isEmpty()
          && !noExportUntil.contains(getWithoutNamespace(childSpanExporter.getInternalSpan().getName()))) {
        childOpenTelemetrySpanExporter.noExportUntil = noExportUntil;
        childOpenTelemetrySpanExporter.spanId = spanId;
        childOpenTelemetrySpanExporter.traceId = traceId;
        childOpenTelemetrySpanExporter.rootSpan = rootSpan;
        childOpenTelemetrySpanExporter.exportable = false;
      }
    }
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public Context getOpenTelemetrySpanContext() {
    return W3CTraceContextPropagator.getInstance().extract(Context.current(), getContext(this),
                                                           OPEN_TELEMETRY_SPAN_GETTER);
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public String getName() {
    return name;
  }

  /**
   * Builder
   */
  public static class OpenTelemetrySpanExportBuilder {

    private InitialSpanInfo initialSpanInfo;
    private InternalSpan internalSpan;
    private String artifactId;
    private String artifactType;
    private OpentelemetryExportQueue opentelemetryExportQueue;

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

    public OpenTelemetrySpanExportBuilder withOpenTelemetryExportQueue(OpentelemetryExportQueue opentelemetryExportQueue) {
      this.opentelemetryExportQueue = opentelemetryExportQueue;
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

      if (opentelemetryExportQueue == null) {
        throw new IllegalArgumentException("Open telemery export queue is null");
      }

      return new OpenTelemetrySpanExporter(internalSpan, initialSpanInfo, artifactId, artifactType, opentelemetryExportQueue);
    }
  }

}
