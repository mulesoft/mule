/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporterVisitor;
import org.mule.runtime.core.internal.profiling.tracing.export.OpenTelemetrySpanExporter;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A wrapper for a span that exports spans on end.
 *
 * @since 4.5.0
 */
public class ExportOnEndSpan implements InternalSpan {

  private final InternalSpan runtimeInternalSpan;
  private final InternalSpanExporter spanExporter;
  private final ChildSpanCustomizationInfo childSpanCustomizationInfo;

  public ExportOnEndSpan(InternalSpan runtimeInternalSpan, EventContext eventContext,
                         InternalSpanExportManager<EventContext> internalSpanExportManager,
                         ChildSpanCustomizationInfo childSpanCustomizationInfo,
                         MuleConfiguration muleConfiguration,
                         boolean exportable,
                         Set<String> noExportUntil) {
    this.runtimeInternalSpan = runtimeInternalSpan;
    this.spanExporter = getExporter(eventContext, internalSpanExportManager, muleConfiguration, exportable, noExportUntil);
    this.childSpanCustomizationInfo = childSpanCustomizationInfo;
  }

  private InternalSpanExporter getExporter(EventContext eventContext,
                                           InternalSpanExportManager<EventContext> internalSpanExportManager,
                                           MuleConfiguration muleConfiguration,
                                           boolean exportable,
                                           Set<String> noExportUntil) {
    return internalSpanExportManager.getInternalSpanExporter(eventContext, muleConfiguration, exportable, noExportUntil, this);
  }

  @Override
  public Span getParent() {
    return runtimeInternalSpan.getParent();
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return runtimeInternalSpan.getIdentifier();
  }

  @Override
  public String getName() {
    return runtimeInternalSpan.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return runtimeInternalSpan.getDuration();
  }

  @Override
  public Set<SpanError> getErrors() {
    return runtimeInternalSpan.getErrors();
  }

  @Override
  public boolean hasErrors() {
    return runtimeInternalSpan.hasErrors();
  }

  @Override
  public void end() {
    runtimeInternalSpan.end();
    spanExporter.export(this);
  }

  @Override
  public void addError(InternalSpanError error) {
    runtimeInternalSpan.addError(error);
  }

  @Override
  public Optional<String> getAttribute(String key) {
    return runtimeInternalSpan.getAttribute(key);
  }

  @Override
  public void addAttribute(String key, String value) {
    runtimeInternalSpan.addAttribute(key, value);
    spanExporter.visit(new AddAttributeVisitor(key, value));
  }

  @Override
  public Map<String, String> attributesAsMap() {
    return runtimeInternalSpan.attributesAsMap();
  }

  @Override
  public <T> T visit(InternalSpanVisitor<T> visitor) {
    return visitor.accept(this);
  }

  public InternalSpanExporter getSpanExporter() {
    return spanExporter;
  }

  @Override
  public ChildSpanCustomizationInfo getChildSpanInfo() {
    return childSpanCustomizationInfo;
  }

  @Override
  public void updateName(String name) {
    runtimeInternalSpan.updateName(name);
    spanExporter.onNameUpdated(name);
  }

  public void addAttributes(Map<String, String> attributes) {
    runtimeInternalSpan.addAttributes(attributes);
  }

  /**
   * Adds attribute to the exporter.
   */
  private static class AddAttributeVisitor implements InternalSpanExporterVisitor<InternalSpanExporter> {

    private final String key;
    private final String value;

    public AddAttributeVisitor(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public InternalSpanExporter accept(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
      openTelemetrySpanExporter.setAttribute(key, value);
      return openTelemetrySpanExporter;
    }
  }
}
