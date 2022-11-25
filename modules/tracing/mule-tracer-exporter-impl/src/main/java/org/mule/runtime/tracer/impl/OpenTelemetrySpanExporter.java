/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl;

import org.mule.runtime.core.api.tracing.customization.NoExportableTillStartExportInfo;
import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import java.util.HashMap;
import java.util.Map;


public class OpenTelemetrySpanExporter implements SpanExporter {

  private final InternalSpan internalSpan;

  private MuleOpenTelemetrySpan openTelemetrySpan;
  private OpenTelemetrySpanExporter rootSpan = this;
  private String rootName;
  private Map<String, String> rootAttributes = new HashMap<>();

  public OpenTelemetrySpanExporter(InternalSpan internalSpan) {
    this.internalSpan = internalSpan;
  }

  @Override
  public void export() {
    openTelemetrySpan.end(internalSpan);
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

  public void configure(StartExportInfo exportSpanCustomizationInfo, String serviceName, boolean isPolicy, boolean isRoot) {
    openTelemetrySpan = OpentelemetrySpanBuilder.getNewOpenTelemetrySpan(internalSpan, exportSpanCustomizationInfo, serviceName,
                                                                         isPolicy, isRoot);
  }

  @Override
  public void setRootName(String rootName) {
    if (openTelemetrySpan.isSetCustomizableInformationCarrier()) {
      openTelemetrySpan.updateName(rootName);
    } else {
      this.rootName = rootName;
    }

  }

  @Override
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    if (openTelemetrySpan.isSetCustomizableInformationCarrier()) {
      openTelemetrySpan.setAttribute(rootAttributeKey, rootAttributeValue);
    } else {
      this.rootAttributes.put(rootAttributeKey, rootAttributeValue);
    }
  }

  @Override
  public void updateChildSpanExporter(SpanExporter childSpanExporter) {
    if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
      OpenTelemetrySpanExporter openTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;
      if (openTelemetrySpanExporter.getOpenTelemetrySpan().isNotIntercepting()) {
        openTelemetrySpanExporter.setRootSpan(rootSpan);
      }

      if (!openTelemetrySpanExporter.getOpenTelemetrySpan().isSetCustomizableInformationCarrier()) {
        openTelemetrySpanExporter.setRootName(rootName);
        openTelemetrySpanExporter.rootAttributes = rootAttributes;
      } else {
        openTelemetrySpanExporter.getOpenTelemetrySpan().updateName(rootName);
        rootAttributes.forEach((key, value) -> openTelemetrySpanExporter.getOpenTelemetrySpan().setAttribute(key, value));
      }
    }

    if (!openTelemetrySpan.getNoExportUntil().isEmpty()
        && !openTelemetrySpan.getNoExportUntil().contains(getWithoutNamespace(childSpanExporter.getInternalSpan().getName()))) {
      if (childSpanExporter instanceof OpenTelemetrySpanExporter) {
        OpenTelemetrySpanExporter openTelemetrySpanExporter = (OpenTelemetrySpanExporter) childSpanExporter;
        openTelemetrySpanExporter.configure(
                                            new NoExportableTillStartExportInfo(openTelemetrySpanExporter
                                                .getOpenTelemetrySpan().getNoExportUntil(), false),
                                            "no-exportable", false, false);
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

  private void setOpentelemetrySpan(MuleOpenTelemetrySpan openTelemetrySpan) {
    this.openTelemetrySpan = openTelemetrySpan;
  }

  @Override
  public InternalSpan getInternalSpan() {
    return internalSpan;
  }
}
