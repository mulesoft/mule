/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.alert.TimedDataAggregation;
import org.mule.runtime.api.alert.TimedDataBuffer;
import org.mule.runtime.api.time.TimeSupplier;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

public class DefaultAlertingSupport implements AlertingSupport {

  private TimeSupplier timeSupplier;

  private Map<String, TimedDataBuffer> timedBuffersPerAlert = new ConcurrentHashMap<>();

  @Override
  public void triggerAlert(String alertName) {
    triggerAlert(alertName, null);
  }

  @Override
  public <T> void triggerAlert(String alertName, T alertData) {
    final var buffer = timedBuffersPerAlert.computeIfAbsent(alertName, k -> new TimedDataBuffer<>(timeSupplier));

    buffer.put(alertData);
  }

  /**
   * Counts each alert for the times it happened in the last 1, 5 and 15 minutes.
   * <p>
   * The time intervals are the same as reported by *nix load average, so that data can be correlated.
   *
   * @return the count aggregation for each alert.
   */
  public Map<String, TimedDataAggregation<Integer>> alertsCountAggregation() {
    return timedBuffersPerAlert.entrySet()
        .stream()
        .collect(toMap(Entry::getKey,
                       e -> e.getValue().<Integer>aggregate(0, (a, t) -> ((Integer) a) + 1)));
  }

  @Inject
  public void setTimeSupplier(TimeSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }
}
