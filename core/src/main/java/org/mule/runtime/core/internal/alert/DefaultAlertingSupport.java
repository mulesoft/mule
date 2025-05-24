/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.alert.TimedDataAggregation;
import org.mule.runtime.api.alert.TimedDataBuffer;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.inject.Inject;

public class DefaultAlertingSupport implements MuleAlertingSupport {

  private TimeSupplier timeSupplier;

  private Map<String, TimedDataBuffer> timedBuffersPerAlert = new ConcurrentHashMap<>();

  @Override
  public void triggerAlert(String alertName) {
    triggerAlert(alertName, null);
  }

  @Override
  public <T> void triggerAlert(String alertName, T alertData) {
    final var buffer = timedBuffersPerAlert.computeIfAbsent(requireNonNull(alertName, "'alertName' cannot be null"),
                                                            k -> new TimedDataBuffer<>(timeSupplier));

    buffer.put(alertData);
  }

  @Override
  public Map<String, TimedDataAggregation<Integer>> alertsCountAggregation() {
    return alertsAggregation(() -> 0, (a, t) -> a + 1);
  }

  @Override
  public <A> Map<String, TimedDataAggregation<A>> alertsAggregation(Supplier<A> baseIntervalAggregationSupplier,
                                                                    BiFunction<A, Object, A> accumulator) {
    final A baseIntevalAggregation = baseIntervalAggregationSupplier.get();
    return timedBuffersPerAlert.entrySet()
        .stream()
        .collect(toMap(Entry::getKey,
                       e -> e.getValue().aggregate(baseIntevalAggregation, accumulator)));
  }

  @Inject
  public void setTimeSupplier(TimeSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }
}
