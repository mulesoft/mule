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

import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding;


/**
 * A mutable mule implementation of the open telemetry {@link TraceState}.
 */
public class MutableMuleTraceState implements TraceState {

  public static final String TRACE_STATE_KEY = "tracestate";

  public static final String ANCESTOR_MULE_SPAN_ID = "ancestor-mule-span-id";
  private final boolean addAncestorMuleSpanIdToTraceState;
  private final boolean propagateAllRemoteTraceContext;

  private final String currentSpanId;

  private Map<String, String> remoteState;

  /**
   * Returns a {@link MutableMuleTraceState} from the representation as a serialized map.
   *
   * @param serializeAsMap                 the serialized trace state
   * @param enableMuleAncestorIdManagement enables mule ancestor id management.
   * @return the resulting {@link MutableMuleTraceState}
   */
  public static MutableMuleTraceState getMutableMuleTraceStateFrom(Map<String, String> serializeAsMap,
                                                                   boolean enableMuleAncestorIdManagement) {
    TraceState remoteTraceState = TraceState.getDefault();
    String traceState = serializeAsMap.get(TRACE_STATE_KEY);
    if (!StringUtils.isEmpty(traceState)) {
      remoteTraceState = W3CTraceContextEncoding.decodeTraceState(traceState);
    }

    // If enableMuleAncestorIdManagement is false, we will set in the trace state all the key/value's from the remote trace
    // context,
    // including the ancestor mule span id if exists.
    // If enableMuleAncestorIdManagement is true, we will use all the key/value's from the remote trace context except
    // the ancestor mule span id and when there there is a current span id to set as ancestor mule span id, we will
    // also include it in the state.
    return new MutableMuleTraceState(remoteTraceState.asMap(), remoteTraceState.get(ANCESTOR_MULE_SPAN_ID),
                                     !enableMuleAncestorIdManagement, enableMuleAncestorIdManagement);
  }

  /**
   * @param remoteState                       the remote trace state. This state came through an endpoint and should be propagated
   *                                          into the mule context.
   * @param currentSpanId                     the current span id to add in the trace state.
   * @param propagateAllRemoteTraceContext    whether all the remote context has to be propagated or we should to take into
   *                                          account the key/value's that belong to mule as vendor.
   * @param addAncestorMuleSpanIdToTraceState whether the set ancestorMuleSpanId has to be set in the trace state.
   */
  private MutableMuleTraceState(Map<String, String> remoteState, String currentSpanId,
                                boolean propagateAllRemoteTraceContext, boolean addAncestorMuleSpanIdToTraceState) {
    this.remoteState = remoteState;
    this.currentSpanId = currentSpanId;
    this.propagateAllRemoteTraceContext = propagateAllRemoteTraceContext;
    this.addAncestorMuleSpanIdToTraceState = addAncestorMuleSpanIdToTraceState;
  }

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
    if (currentSpanId != null) {
      return false;
    }
    return remoteState.isEmpty();
  }

  @Override
  public void forEach(BiConsumer<String, String> biConsumer) {
    // we don't propagate the remote mule ancestor span id
    // from the remote context unless we indicate to propagate the it
    // in an explicit way.
    remoteState.forEach((key, value) -> {
      if (propagateAllRemoteTraceContext || !key.equals(ANCESTOR_MULE_SPAN_ID)) {
        biConsumer.accept(key, value);
      }
    });

    // If we indicate in an explicit way that the ancestor mule span id
    // has to be in this trace state, we add it.
    if (currentSpanId != null && addAncestorMuleSpanIdToTraceState) {
      biConsumer.accept(ANCESTOR_MULE_SPAN_ID, currentSpanId);
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
    return new MutableMuleTraceState(remoteState, ancestorSpanId, propagateAllRemoteTraceContext,
                                     addAncestorMuleSpanIdToTraceState);
  }

  /**
   * propagates the remote context without the ancestor span id.
   */
  public void propagateRemoteContext(MutableMuleTraceState targetMuleTraceState) {
    targetMuleTraceState.remoteState = remoteState;
  }
}
