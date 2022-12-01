/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span;

import static java.util.Collections.emptyMap;

import static java.util.Optional.empty;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link InternalSpan} used internally by the runtime. It defines an extension of the span contract that is only used
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
   * Adds to the Span the provided {@link InternalSpanError}.
   *
   * @param error Error that will be added to the Span.
   */
  void addError(InternalSpanError error);

  /**
   * Add an attribute
   *
   * @param key   key for the attribute
   * @param value the value for the attribute added
   */
  default void addAttribute(String key, String value) {}

  /**
   * @return the attribute corresponding to the {@param key}
   */
  default Optional<String> getAttribute(String key) {
    return empty();
  }

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
   * Updates the name for the current {@link InternalSpan}
   *
   * @param name the name to set to the current span.
   */
  void updateName(String name);


  /**
   * Gets the span exporter
   *
   * @return the span exporter.
   */
  SpanExporter getSpanExporter();

  /**
   * Gets the attributes.
   *
   * @return a map with the span attributes.
   */
  Map<String, String> getAttributes();

  /**
   * Updates the child {@link InternalSpan}.
   *
   * @param childInternalSpan the child {@link InternalSpan}.
   */
  default void updateChildSpanExporter(InternalSpan childInternalSpan) {}

  /**
   * Serializes the span as a map.
   *
   * @return the serialized span.
   */
  Map<String, String> serializeAsMap();

  /**
   * Sets the root name in the local trace for the {@link InternalSpan}. This is useful in case a root element sets a name for the
   * flow, and it has to be propagated to the flow span. This propagation is needed, for example, if a source sets a name for
   * complying with semantic conventions for the flow and there is a policy applied to the source.
   *
   * @param rootName the root name.
   */
  default void updateRootName(String rootName) {}

  /**
   * Sets a root attribute in the local trace for {@link InternalSpan} This is useful in case a root element sets an attribute for
   * the flow, and it has to be propagated to the flow span. This propagation is needed, for example, if a source sets a name for
   * complying with semantic conventions for the flow and there is a policy applied to the source.
   *
   * @param rootAttributeKey   the key for root attribute.
   * @param rootAttributeValue the value for the root attribute.
   */
  default void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {}


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
    public void addError(InternalSpanError error) {
      // Nothing to do.
    }

    @Override
    public void updateName(String name) {
      // Nothing to do.
    }

    @Override
    public SpanExporter getSpanExporter() {
      return null;
    }

    @Override
    public Map<String, String> getAttributes() {
      return emptyMap();
    }

    @Override
    public Map<String, String> serializeAsMap() {
      return emptyMap();
    }
  }
}
