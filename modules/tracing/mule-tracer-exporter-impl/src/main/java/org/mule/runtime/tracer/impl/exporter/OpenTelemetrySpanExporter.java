/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.tracer.impl.exporter.MuleOpenTelemetrySpanProvider.getNewOpenTelemetrySpan;

public class OpenTelemetrySpanExporter implements SpanExporter {

  public static OpenTelemetrySpanExportBuilder builder() {
    return new OpenTelemetrySpanExportBuilder();
  }

  public static final String NO_EXPORTABLE_SERVICE = "no-exportable-service";

  private final InternalSpan internalSpan;
  private final StartSpanInfo startSpanInfo;
  private final String artifactId;
  private final String artifactType;

  private MuleOpenTelemetrySpan openTelemetrySpan;
  private OpenTelemetrySpanExporter rootSpan = this;
  private String rootName;
  private final Map<String, String> rootAttributes = new HashMap<>();

  private OpenTelemetrySpanExporter(InternalSpan internalSpan,
                                    StartSpanInfo startSpanInfo,
                                    String artifactId,
                                    String artifactType) {
    this.internalSpan = internalSpan;
    this.startSpanInfo = startSpanInfo;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
  }

  @Override
  public void export() {
    openTelemetrySpan.end(internalSpan, startSpanInfo, artifactId, artifactType);
  }

  @Override
  public void updateNameForExport(String newName) {
    if (rootSpan != this) {
      rootSpan.updateNameForExport(newName);
    } else {
      openTelemetrySpan.updateName(newName);
    }
  }

  @Override
  public Map<String, String> exportedSpanAsMap() {
    return openTelemetrySpan.getDistributedTraceContextMap();
  }

  public MuleOpenTelemetrySpan getOpenTelemetrySpan() {
    return openTelemetrySpan;
  }

  @Override
  public void setRootName(String rootName) {
    if (openTelemetrySpan.isRoot()) {
      openTelemetrySpan.updateName(rootName);
    } else {
      this.rootName = rootName;
    }
  }

  @Override
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    if (openTelemetrySpan.isRoot()) {
      openTelemetrySpan.setAttribute(rootAttributeKey, rootAttributeValue);
    } else {
      this.rootAttributes.put(rootAttributeKey, rootAttributeValue);
    }
  }

  @Override
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter childOpenTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;

      // If it is a policy span, propagate the rootSpan.
      if (childOpenTelemetrySpanExporter.getOpenTelemetrySpan().onlyPropagateNamesAndAttributes()) {
        childOpenTelemetrySpanExporter.setRootSpan(rootSpan);
      }

      // Propagates the root name until it finds a root.
      if (rootName != null) {
        childOpenTelemetrySpanExporter.setRootName(rootName);
        rootAttributes.forEach(childOpenTelemetrySpanExporter::setRootAttribute);
      }

      // No export if no export till reset.
      if (!openTelemetrySpan.getNoExportUntil().isEmpty()
          && !openTelemetrySpan.getNoExportUntil().contains(getWithoutNamespace(childSpanExporter.getInternalSpan().getName()))) {
        childOpenTelemetrySpanExporter.openTelemetrySpan = getNewOpenTelemetrySpan(internalSpan,
                                                                                   new NoExportableStartSpanInfo(childOpenTelemetrySpanExporter
                                                                                       .getOpenTelemetrySpan()
                                                                                       .getNoExportUntil()),
                                                                                   NO_EXPORTABLE_SERVICE);
      }
    }

  }

  private void setRootSpan(OpenTelemetrySpanExporter openTelemetrySpan) {
    this.rootSpan = openTelemetrySpan;
  }

  private String getWithoutNamespace(String name) {
    int index = name.lastIndexOf(":");
    if (index != -1) {
      return name.substring(index + 1);
    } else {
      return name;
    }
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }

  public static class OpenTelemetrySpanExportBuilder {

    private StartSpanInfo startSpanInfo;
    private InternalSpan internalSpan;
    private String artifactId;
    private String artifactType;

    public OpenTelemetrySpanExportBuilder withStartSpanInfo(StartSpanInfo startSpanInfo) {
      this.startSpanInfo = startSpanInfo;
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

    public OpenTelemetrySpanExporter build() {

      if (startSpanInfo == null) {
        throw new IllegalArgumentException("Start span info is null");
      }

      if (artifactId == null) {
        throw new IllegalArgumentException("Artifact id is null");
      }

      if (artifactType == null) {
        throw new IllegalArgumentException("Artifact type is null");
      }

      OpenTelemetrySpanExporter openTelemetrySpanExporter =
          new OpenTelemetrySpanExporter(internalSpan, startSpanInfo, artifactId, artifactType);
      openTelemetrySpanExporter.openTelemetrySpan = getNewOpenTelemetrySpan(internalSpan, startSpanInfo, artifactId);
      return openTelemetrySpanExporter;
    }
  }
}
