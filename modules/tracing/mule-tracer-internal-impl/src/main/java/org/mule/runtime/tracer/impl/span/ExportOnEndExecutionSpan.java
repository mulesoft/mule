/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.tracer.impl.clock.Clock.getDefault;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.SpanAttribute;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link Span} that represents the trace corresponding to the execution of mule flow or component.
 *
 * @since 4.5.0
 */
public class ExportOnEndExecutionSpan implements InternalSpan {

  public static final String SPAN_KIND = "span.kind.override";
  public static final String STATUS = "status.override";
  private final InitialSpanInfo initialSpanInfo;
  private final SpanExporter spanExporter;
  private SpanError lastError;
  private final InternalSpan parent;
  private final Long startTime;
  private Long endTime;
  private final List<SpanAttribute<String>> additionalAttributes = new ArrayList<>();

  private ExportOnEndExecutionSpan(SpanExporterFactory spanExporterFactory, InitialSpanInfo initialSpanInfo, Long startTime,
                                   InternalSpan parent) {
    this.initialSpanInfo = initialSpanInfo;
    this.startTime = startTime;
    this.parent = parent;
    this.spanExporter = spanExporterFactory.getSpanExporter(this, initialSpanInfo);
  }

  public static InternalSpan createExportOnEndExecutionSpan(SpanExporterFactory spanExporterFactory, InternalSpan parentSpan,
                                                            InitialSpanInfo initialSpanInfo) {
    requireNonNull(spanExporterFactory);
    requireNonNull(initialSpanInfo);
    ExportOnEndExecutionSpan exportOnEndExecutionSpan = new ExportOnEndExecutionSpan(spanExporterFactory, initialSpanInfo,
                                                                                     getDefault().now(),
                                                                                     parentSpan);
    return parentSpan.onChild(exportOnEndExecutionSpan);
  }

  public SpanExporter getSpanExporter() {
    return spanExporter;
  }

  @Override
  public InternalSpan onChild(InternalSpan child) {
    if (child instanceof ExportOnEndExecutionSpan) {
      spanExporter.updateChildSpanExporter(((ExportOnEndExecutionSpan) child).getSpanExporter());
    }
    return child;
  }

  @Override
  public void updateRootName(String name) {
    spanExporter.setRootName(name);
  }

  @Override
  public void end() {
    end(getDefault().now());
  }

  @Override
  public void end(long endTime) {
    this.endTime = endTime;
    this.spanExporter.export();
  }

  @Override
  public void addError(InternalSpanError error) {
    this.lastError = error;
    spanExporter.onError(error);
  }

  @Override
  public void updateName(String name) {
    this.spanExporter.updateNameForExport(name);
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    initialSpanInfo.forEachAttribute(biConsumer);
    if (!additionalAttributes.isEmpty()) {
      additionalAttributes.forEach(spanAttribute -> biConsumer.accept(spanAttribute.getKey(), spanAttribute.getValue()));
    }
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return spanExporter.exportedSpanAsMap();
  }

  @Override
  public boolean hasErrors() {
    return lastError != null;
  }

  @Override
  public Span getParent() {
    return parent;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return getSpanExporter().getSpanIdentifier();
  }

  @Override
  public String getName() {
    return initialSpanInfo.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return new DefaultSpanDuration(startTime, endTime);
  }

  @Override
  public List<SpanError> getErrors() {
    if (lastError != null) {
      return singletonList(lastError);
    }

    return emptyList();
  }

  @Override
  public int getAttributesCount() {
    return initialSpanInfo.getInitialAttributesCount() + additionalAttributes.size();
  }

  @Override
  public void setRootAttribute(SpanAttribute<String> spanAttribute) {
    spanExporter.setRootAttribute(spanAttribute);
  }

  /**
   * An default implementation for a {@link SpanDuration}
   */
  private static class DefaultSpanDuration implements SpanDuration {

    private final Long startTime;
    private final Long endTime;

    public DefaultSpanDuration(Long startTime, Long endTime) {
      this.startTime = startTime;
      this.endTime = endTime;
    }

    @Override
    public Long getStart() {
      return startTime;
    }

    @Override
    public Long getEnd() {
      return endTime;
    }
  }

  @Override
  public void addAttribute(SpanAttribute<String> spanAttribute) {
    if (!spanAttribute.getKey().equals(SPAN_KIND) && !spanAttribute.getKey().equals(STATUS) && !initialSpanInfo.isPolicySpan()) {
      additionalAttributes.add(spanAttribute);
    }
    spanExporter.onAdditionalAttribute(spanAttribute);
  }

}

