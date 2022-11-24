/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span;

import static org.mule.runtime.tracer.api.span.exporter.SpanExporter.NOOP_EXPORTER;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.impl.clock.Clock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link Span} that represents the trace corresponding to the execution of mule flow or component.
 *
 * @since 4.5.0
 */
public class ExecutionSpan implements InternalSpan {

  private final StartSpanInfo spanCustomizationInfo;
  private SpanExporter spanExporter = NOOP_EXPORTER;

  public static ExecutionSpanBuilder getExecutionSpanBuilder() {
    return new ExecutionSpanBuilder();
  }

  private final InternalSpan parent;
  private final Long startTime;
  private Long endTime;
  private final Map<String, String> attributes = new HashMap<>();
  private final Set<SpanError> errors = new HashSet<>();

  private ExecutionSpan(StartSpanInfo spanCustomizationInfo, Long startTime,
                        InternalSpan parent) {
    this.spanCustomizationInfo = spanCustomizationInfo;
    this.startTime = startTime;
    this.parent = parent;
  }

  @Override
  public Span getParent() {
    return parent;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return null;
  }

  @Override
  public String getName() {
    return spanCustomizationInfo.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return new DefaultSpanDuration(startTime, endTime);
  }

  @Override
  public Set<SpanError> getErrors() {
    return errors;
  }

  @Override
  public void setRootAttribute(String s, String s1) {
    spanExporter.setRootAttribute(s, s1);
  }

  @Override
  public void updateRootName(String name) {
    spanExporter.setRootName(name);
  }

  @Override
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Override
  public void end() {
    this.endTime = Clock.getDefault().now();
    this.spanExporter.export();
  }

  @Override
  public void addError(InternalSpanError error) {
    this.errors.add(error);
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
  public Map<String, String> getAttributes() {
    return attributes;
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
    attributes.put(key, value);
  }

  @Override
  public Optional<String> getAttribute(String key) {
    return ofNullable(attributes.get(key));
  }

  @Override
  public void updateChildSpanExporter(InternalSpan internalSpan) {
    spanExporter.updateChildSpanExporter(internalSpan.getSpanExporter());
  }

  /**
   * x A Builder for {@link ExecutionSpan}
   *
   * @since 4.5.0
   */
  public static class ExecutionSpanBuilder {

    public static final String ARTIFACT_ID_IS_NULL_MESSAGE = "Artifact id is null";
    private String artifactId;
    private InternalSpan parent;
    private Long startTime;
    private SpanExporterFactory spanExporterFactory;
    private StartSpanInfo spanCustomizationInfo;

    private ExecutionSpanBuilder() {}

    public ExecutionSpanBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public ExecutionSpanBuilder withSpanCustomizationInfo(StartSpanInfo spanCustomizationInfo) {
      this.spanCustomizationInfo = spanCustomizationInfo;
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

    private SpanExporter resolveSpanExporter(SpanExporterFactory spanExporterFactory, ExecutionSpan executionSpan,
                                             StartSpanInfo exportStartSpanInfo) {
      return spanExporterFactory.getSpanExporter(executionSpan, exportStartSpanInfo);
    }

    public ExecutionSpan build() {
      if (artifactId == null) {
        throw new IllegalArgumentException(ARTIFACT_ID_IS_NULL_MESSAGE);
      }

      if (startTime == null) {
        startTime = Clock.getDefault().now();
      }

      if (spanExporterFactory == null) {
        throw new IllegalArgumentException("there is no span factory");
      }

      ExecutionSpan executionSpan = new ExecutionSpan(spanCustomizationInfo,
                                                      startTime,
                                                      parent);


      executionSpan.spanExporter = resolveSpanExporter(spanExporterFactory,
                                                       executionSpan,
                                                       spanCustomizationInfo);

      return executionSpan;

    }

  }
}

