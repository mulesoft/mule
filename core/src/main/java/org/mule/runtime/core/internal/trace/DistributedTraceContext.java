/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.trace;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CurrentSpanAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a state that can be propagated through runtime boundaries.
 *
 * It is divided in two pieces: (1) any implementation specific fields that are useful to contextualize a {@link Event} across
 * Mule boundaries. (2) the "baggage" that propagates user level properties. The baggage can be used to propagate cross-cutting
 * concerns in the way of <a href="https://opentelemetry.io/docs/reference/specification/overview/#baggage-signal">OpenTelemetry
 * Baggage</a> does.
 *
 * @since 1.5.0
 */
public interface DistributedTraceContext extends CurrentSpanAware {

  /**
   * Returns the optional value for a trace context key. This is agnostic to the tracing standard that we are using (for example,
   * <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>).
   *
   * @param key that represents the name of key that is used for trace (for example, traceId, spanId, correlationId, etc.)
   * @return the optional value corresponding to the keys passed as parameters
   */
  Optional<String> getTraceFieldValue(String key);

  /**
   * @return returns the map of the trace context fields.
   */
  Map<String, String> tracingFieldsAsMap();

  /**
   * @param key that represents the the baggage item.
   * @return returns an optional baggage item corresponding to the key.
   */
  Optional<String> getBaggageItem(String key);

  /**
   * @return the map of the baggage items.
   */
  Map<String, String> baggageItemsAsMap();

  /**
   * @return a copy of the {@link DistributedTraceContext}
   */
  DistributedTraceContext copy();

  void endCurrentContextSpan();

  /**
   * @return a {@link DistributedTraceContext} that has no fields nor baggage set.
   */
  static DistributedTraceContext emptyDistributedEventContext() {
    return new DistributedTraceContext() {

      @Override
      public Optional<String> getTraceFieldValue(String key) {
        return empty();
      }

      @Override
      public Map<String, String> tracingFieldsAsMap() {
        return emptyMap();
      }

      @Override
      public Optional<String> getBaggageItem(String key) {
        return empty();
      }

      @Override
      public Map<String, String> baggageItemsAsMap() {
        return emptyMap();
      }

      @Override
      public DistributedTraceContext copy() {
        return this;
      }

      @Override
      public void endCurrentContextSpan() {

      }

      @Override
      public void setCurrentSpan(InternalSpan span) {}

      @Override
      public Optional<InternalSpan> getCurrentSpan() {
        return empty();
      }
    };
  }
}
