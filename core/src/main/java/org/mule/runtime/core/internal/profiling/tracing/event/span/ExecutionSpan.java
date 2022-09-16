/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static java.util.Optional.ofNullable;

import static com.google.common.collect.ImmutableMap.copyOf;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.core.internal.profiling.tracing.Clock;

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

  private String name;
  private final SpanIdentifier identifier;
  private final InternalSpan parent;
  private final Long startTime;
  private Long endTime;
  private final Map<String, String> attributes = new HashMap<>();
  private final Set<SpanError> errors = new HashSet<>();

  public ExecutionSpan(String name, SpanIdentifier identifier, Long startTime, Long endTime,
                       InternalSpan parent) {
    this.name = name;
    this.identifier = identifier;
    this.startTime = startTime;
    this.endTime = endTime;
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
    return name;
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
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Override
  public void end() {
    this.endTime = Clock.getDefault().now();
  }

  @Override
  public void addError(InternalSpanError error) {
    this.errors.add(error);
  }

  @Override
  public <T> T visit(InternalSpanVisitor<T> visitor) {
    return visitor.accept(this);
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    this.attributes.putAll(attributes);
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    this.attributes.put(key, value);
  }

  @Override
  public void setCurrentSpanName(String name) {
    this.name = name;
  }

  @Override
  public Map<String, String> attributesAsMap() {
    return copyOf(attributes);
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
}
