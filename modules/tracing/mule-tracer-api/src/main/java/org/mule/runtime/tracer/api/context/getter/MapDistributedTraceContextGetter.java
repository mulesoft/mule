/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.context.getter;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link DistributedTraceContextGetter} that receives a Map<String, String> to get the data from
 *
 * @since 4.5.0
 */
public class MapDistributedTraceContextGetter implements DistributedTraceContextGetter {

  private final Map<String, String> serializedMap;

  public MapDistributedTraceContextGetter(Map<String, String> serializedMap) {
    this.serializedMap = serializedMap;
  }

  @Override
  public Iterable<String> keys() {
    return serializedMap.keySet();
  }

  @Override
  public Optional<String> get(String key) {
    return ofNullable(serializedMap.get(key));
  }

  @Override
  public boolean isEmptyDistributedTraceContext() {
    return serializedMap.isEmpty();
  }
}
