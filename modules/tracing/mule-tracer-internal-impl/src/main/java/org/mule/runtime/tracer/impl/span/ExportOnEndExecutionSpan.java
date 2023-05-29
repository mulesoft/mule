/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span;

import static org.mule.runtime.tracer.api.span.exporter.SpanExporter.NOOP_EXPORTER;
import static org.mule.runtime.tracer.impl.clock.Clock.getDefault;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import java.util.HashMap;
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
  private SpanIdentifier identifier;
  private SpanExporter spanExporter = NOOP_EXPORTER;
  private SpanError lastError;

  public static ExecutionSpanBuilder getExecutionSpanBuilder() {
    return new ExecutionSpanBuilder();
  }

  private final InternalSpan parent;
  private final Long startTime;
  private Long endTime;
  private final Map<String, String> additionalAttributes = new HashMap<>();

  private ExportOnEndExecutionSpan(InitialSpanInfo initialSpanInfo, Long startTime,
                                   InternalSpan parent) {
    this.initialSpanInfo = initialSpanInfo;
    this.startTime = startTime;
    this.parent = parent;
  }

  @Override
  public Span getParent() {
    return parent;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return identifier;
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
  public void setRootAttribute(String s, String s1) {
    spanExporter.setRootAttribute(s, s1);
  }

  @Override
  public int getAttributesCount() {
    return initialSpanInfo.getInitialAttributesCount() + additionalAttributes.size();
  }


  @Override
  public void updateRootName(String name) {
    spanExporter.setRootName(name);
  }

  @Override
  public boolean hasErrors() {
    return lastError != null;
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
  public SpanExporter getSpanExporter() {
    return spanExporter;
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    initialSpanInfo.forEachAttribute(biConsumer);
    if (!additionalAttributes.isEmpty()) {
      additionalAttributes.forEach(biConsumer);
    }
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return spanExporter.exportedSpanAsMap();
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
  public void addAttribute(String key, String value) {
    if (!key.equals(SPAN_KIND) && !key.equals(STATUS) && !initialSpanInfo.isPolicySpan()) {
      additionalAttributes.put(key, value);
    }
    spanExporter.onAdditionalAttribute(key, value);
  }

  @Override
  public InternalSpan updateChildSpanExporter(InternalSpan internalSpan) {
    spanExporter.updateChildSpanExporter(internalSpan.getSpanExporter());
    return internalSpan;
  }

  private void updateIdentifierBasedOnExporter() {
    this.identifier = spanExporter.getSpanIdentifierBasedOnExport();
  }

  /**
   * x A Builder for {@link ExportOnEndExecutionSpan}
   *
   * @since 4.5.0
   */
  public static class ExecutionSpanBuilder {

    public static final String THERE_IS_NO_SPAN_FACTORY_MESSAGE = "there is no span factory";

    private InternalSpan parent;
    private Long startTime;
    private SpanExporterFactory spanExporterFactory;
    private InitialSpanInfo initialSpanInfo;

    private ExecutionSpanBuilder() {}

    public ExecutionSpanBuilder withStartSpanInfo(InitialSpanInfo spanCustomizationInfo) {
      this.initialSpanInfo = spanCustomizationInfo;
      return this;
    }

    public ExecutionSpanBuilder withParentSpan(InternalSpan parent) {
      this.parent = parent;
      return this;
    }

    public ExecutionSpanBuilder withSpanExporterFactory(SpanExporterFactory spanExporterFactory) {
      this.spanExporterFactory = spanExporterFactory;
      return this;
    }

    public ExportOnEndExecutionSpan build() {
      if (startTime == null) {
        startTime = getDefault().now();
      }

      if (spanExporterFactory == null) {
        throw new IllegalArgumentException(THERE_IS_NO_SPAN_FACTORY_MESSAGE);
      }

      ExportOnEndExecutionSpan exportOnEndExecutionSpan = new ExportOnEndExecutionSpan(initialSpanInfo,
                                                                                       startTime,
                                                                                       parent);


      exportOnEndExecutionSpan.spanExporter = spanExporterFactory.getSpanExporter(exportOnEndExecutionSpan, initialSpanInfo);
      exportOnEndExecutionSpan.updateIdentifierBasedOnExporter();

      return exportOnEndExecutionSpan;
    }

  }
}

