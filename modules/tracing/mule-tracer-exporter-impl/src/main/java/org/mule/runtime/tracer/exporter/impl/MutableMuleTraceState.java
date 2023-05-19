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
import org.apache.commons.lang.NotImplementedException;


/**
 * A mutable mule implementation of the open telemetry {@link TraceState}.
 */
public class MutableMuleTraceState implements TraceState {

  public static final String TRACE_STATE_KEY = "tracestate";

  public static final String ANCESTOR_MULE_SPAN_ID = "ancestor-mule-span-id";

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
    return new MutableMuleTraceState(remoteTraceState.asMap());
  }

  private final Map<String, String> state;

  public MutableMuleTraceState(Map<String, String> state) {
    this.state = state;
  }

  public MutableMuleTraceState() {
    this.state = new HashMap<>();
  }

  @Nullable
  @Override
  public String get(String key) {
    return state.get(key);
  }

  @Override
  public int size() {
    return state.size();
  }

  @Override
  public boolean isEmpty() {
    return state.isEmpty();
  }

  @Override
  public void forEach(BiConsumer<String, String> biConsumer) {
    state.forEach(biConsumer);
  }

  @Override
  public Map<String, String> asMap() {
    return state;
  }

  @Override
  public TraceStateBuilder toBuilder() {
    // This is only used internally and it is not implemented.
    throw new NotImplementedException();
  }

  public void put(String key, String value) {
    state.put(key, value);
  }

  /**
   * @param ancestorSpanId span id xthat will be added as an ancestor.
   * @return the trace state with the ancestor key/value.
   */
  public TraceState withAncestor(String ancestorSpanId) {
    Map<String, String> newMap = new HashMap<>(state);
    newMap.put(ANCESTOR_MULE_SPAN_ID, ancestorSpanId);
    return new MutableMuleTraceState(newMap);
  }

  /**
   * @ @return adds all the key values of the current span.
   */
  public void copyAllKeyValuesWithoutAncestor(MutableMuleTraceState targetMuleTraceState) {
    targetMuleTraceState.state.putAll(state);
    targetMuleTraceState.state.remove(ANCESTOR_MULE_SPAN_ID);
  }
}
