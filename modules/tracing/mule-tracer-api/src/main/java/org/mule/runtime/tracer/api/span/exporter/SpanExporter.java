/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span.exporter;

import static org.mule.runtime.api.profiling.tracing.SpanIdentifier.INVALID_SPAN_IDENTIFIER;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.SpanAttribute;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;

import java.util.Map;

/**
 * An exporter for {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface SpanExporter {

  SpanExporter NOOP_EXPORTER = new SpanExporter() {

    @Override
    public void export() {
      // Nothing to do.
    }

    @Override
    public void updateNameForExport(String newName) {
      // Nothing to do.
    }

    @Override
    public Map<String, String> exportedSpanAsMap() {
      return emptyMap();
    }

    @Override
    public InternalSpan getInternalSpan() {
      return null;
    }

    @Override
    public SpanIdentifier getSpanIdentifier() {
      return INVALID_SPAN_IDENTIFIER;
    }
  };

  /**
   * Exports the {@link InternalSpan}.
   */
  void export();

  /**
   * Indicates that the span should be exported using a new name.
   *
   * @param newName the new name.
   */
  void updateNameForExport(String newName);

  /**
   * @return the exported span as a map.
   */
  Map<String, String> exportedSpanAsMap();

  /**
   * Updates the exporter of a child {@link InternalSpan}.
   *
   * @param childSpanExporter the child {@link InternalSpan} exporter.
   */
  default void updateChildSpanExporter(SpanExporter childSpanExporter) {}

  /**
   * @return the {@link InternalSpan} to export.
   */
  InternalSpan getInternalSpan();

  /**
   * Sets a root attribute in the local trace for the exporter. This is useful in case a root element sets an attribute for the
   * flow, and it has to be propagated to the flow span. This propagation is needed, for example, if a source sets a name for
   * complying with semantic conventions for the flow and there is a policy applied to the source.
   *
   * @param rootAttributeKey   the key for root attribute.
   * @param rootAttributeValue the value for the root attribute.
   */
  default void setRootAttribute(SpanAttribute<String> rootAttribute) {}

  /**
   * Sets the root name in the local trace for the exporter. This is useful in case a root element sets a name for the flow, and
   * it has to be propagated to the flow span. This propagation is needed, for example, if a source sets a name for complying with
   * semantic conventions for the flow and there is a policy applied to the source.
   *
   * @param rootName the root name.
   */
  default void setRootName(String rootName) {}

  /**
   * Operation to do when an attribute is added.
   *
   * @param key   the key for the new attribute added
   * @param value the value for the attribute
   */
  default void onAdditionalAttribute(SpanAttribute<String> additionalAttribute) {}

  /**
   * Operation to do when an attribute an error occurs.
   *
   * @param error the error.
   */
  default void onError(InternalSpanError error) {}

  /**
   * Updates parent span from a map
   *
   * @param spanSerializedAsMap the serialization map that represents the span in W3C format.
   */
  default void updateParentSpanFrom(Map<String, String> spanSerializedAsMap) {}

  /**
   * @return a {@link SpanIdentifier} based on the export.
   */
  SpanIdentifier getSpanIdentifier();
}
