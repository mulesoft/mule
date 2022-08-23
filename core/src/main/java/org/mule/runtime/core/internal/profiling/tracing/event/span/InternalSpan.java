/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link InternalSpan }used internally by the runtime. It defines an extension of the span contract that is only used
 * internally.
 *
 * @since 4.5.0
 */
public interface InternalSpan extends Span {

  /**
   * Ends the span.
   */
  void end();


  /**
   * Ends the span, previously recording the provided error.
   */
  void recordError(InternalSpanError error);

  /**
   * @return the attribute corresponding to the {@param key}
   */
  default Optional<String> getAttribute(String key) {
    return empty();
  }

  /**
   * Add an attribute
   *
   * @param key   key for the attribute
   * @param value the value for the attribute added
   */
  default void addAttribute(String key, String value) {

  }

  /**
   * @return the attributes as a map.
   */
  default Map<String, String> attributesAsMap() {
    return emptyMap();
  }

  /**
   * @param visitor the visitor
   * @param <T>     the type to return
   * @return the result.
   */
  <T> T visit(InternalSpanVisitor<T> visitor);

  /**
   * @param span the {@link Span}
   * @return the span as internal.
   */
  static InternalSpan getAsInternalSpan(Span span) {
    if (span == null) {
      return null;
    }

    if (span instanceof InternalSpan) {
      return (InternalSpan) span;
    }

    return new SpanInternalWrapper(span);
  }

  /**
   * @return {@link ChildSpanCustomizationInfo} representing additional about the creation of children spans.
   */
  default ChildSpanCustomizationInfo getChildSpanInfo() {
    return getDefaultChildSpanInfo();
  }

  /**
   * A wrapper as InternalSpan for other type of {@link Span}
   */
  class SpanInternalWrapper implements InternalSpan {

    private final Span span;

    private SpanInternalWrapper(Span span) {
      this.span = span;
    }

    @Override
    public Span getParent() {
      return span.getParent();
    }

    @Override
    public SpanIdentifier getIdentifier() {
      return span.getIdentifier();
    }

    @Override
    public String getName() {
      return span.getName();
    }

    @Override
    public SpanDuration getDuration() {
      return span.getDuration();
    }

    @Override
    public Set<SpanError> getErrors() {
      return span.getErrors();
    }

    @Override
    public boolean hasErrors() {
      return span.hasErrors();
    }

    @Override
    public void end() {
      // Nothing to do.
    }

    @Override
    public void recordError(InternalSpanError error) {
      // Nothing to do.
    }

    @Override
    public <T> T visit(InternalSpanVisitor<T> visitor) {
      return visitor.accept(this);
    }
  }
}
