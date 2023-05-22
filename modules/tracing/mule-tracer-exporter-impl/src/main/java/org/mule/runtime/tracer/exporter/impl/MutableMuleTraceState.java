/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl;

import org.mule.runtime.core.api.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding;


/**
 * A mutable mule implementation of the open telemetry {@link TraceState}.
 */
public class MutableMuleTraceState implements TraceState {

  public static final String TRACE_STATE_KEY = "tracestate";

  public static final String ANCESTOR_MULE_SPAN_ID = "ancestor-mule-span-id";
  private String ancestorMuleSpanId;

  private Map<String, String> remoteState;

  /**
   * Returns a {@link MutableMuleTraceState} from the representation as a serialized map.
   *
   * @param serializeAsMap the serialized trace state
   * @return the resulting {@link MutableMuleTraceState}
   */
  public static MutableMuleTraceState getMutableMuleTraceStateFrom(Map<String, String> serializeAsMap) {
    TraceState remoteTraceState = TraceState.getDefault();
    String traceState = serializeAsMap.get(TRACE_STATE_KEY);
    if (!StringUtils.isEmpty(traceState)) {
      remoteTraceState = W3CTraceContextEncoding.decodeTraceState(traceState);
    }
    return new MutableMuleTraceState(remoteTraceState.asMap(), remoteTraceState.get(ANCESTOR_MULE_SPAN_ID));
  }

  public MutableMuleTraceState(Map<String, String> remoteState, String ancestorMuleSpanId) {
    this.remoteState = remoteState;
    this.ancestorMuleSpanId = ancestorMuleSpanId;
  }

  public MutableMuleTraceState() {
    this.remoteState = new HashMap<>();
  }

  @Nullable
  @Override
  public String get(String key) {
    return remoteState.get(key);
  }

  @Override
  public int size() {
    return remoteState.size();
  }

  @Override
  public boolean isEmpty() {
    if (ancestorMuleSpanId != null) {
      return false;
    }
    return remoteState.isEmpty();
  }

  @Override
  public void forEach(BiConsumer<String, String> biConsumer) {
    // From the remote state we don't export the ancestor.
    remoteState.forEach((key, value) -> {
      if (!key.equals(ANCESTOR_MULE_SPAN_ID)) {
        biConsumer.accept(key, value);
      }
    });

    if (ancestorMuleSpanId != null) {
      biConsumer.accept(ANCESTOR_MULE_SPAN_ID, ancestorMuleSpanId);
    }
  }

  @Override
  public Map<String, String> asMap() {
    Map<String, String> mapWithAncestor = new HashMap<>();
    this.forEach(mapWithAncestor::put);
    return mapWithAncestor;
  }

  @Override
  public TraceStateBuilder toBuilder() {
    // This is only used internally and it is not implemented.
    throw new UnsupportedOperationException();
  }

  public void put(String key, String value) {
    remoteState.put(key, value);
  }

  /**
   * @param ancestorSpanId span id that will be added as an ancestor.
   * @return the trace state with the ancestor key/value.
   */
  public TraceState withAncestor(String ancestorSpanId) {
    return new MutableMuleTraceState(remoteState, ancestorSpanId);
  }

  /**
   * propagates the remote context without the ancestor span id.
   */
  public void propagateRemoteContext(MutableMuleTraceState targetMuleTraceState) {
    targetMuleTraceState.remoteState = remoteState;
  }
}
