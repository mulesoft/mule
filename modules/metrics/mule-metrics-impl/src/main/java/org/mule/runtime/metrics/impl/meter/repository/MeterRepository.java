/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter.repository;

import org.mule.runtime.metrics.api.meter.Meter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A repository for {@link Meter}.
 */
public class MeterRepository<T extends Meter> {

  Map<String, T> meterMap = new ConcurrentHashMap<>();

  /**
   * @param name            the name of the meter
   * @param builderFunction the builder function to create {@link Meter} if not present.
   */
  public T getOrCreate(String name, Function<String, T> builderFunction) {
    return meterMap.computeIfAbsent(name, builderFunction);
  }

}
