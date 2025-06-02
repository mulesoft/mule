/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_ASYNC_LOGGER_RINGBUFFER_FULL;
import static org.mule.runtime.core.internal.artifact.ArtifactClassLoaderFinder.artifactClassLoaderFinder;
import static org.mule.runtime.module.log4j.api.MuleAlertingAsyncQueueFullPolicy.register;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.alert.TimedDataAggregation;
import org.mule.runtime.api.alert.TimedDataBuffer;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;

import jakarta.inject.Inject;

public class DefaultAlertingSupport implements MuleAlertingSupport, Initialisable {

  private static final Logger LOGGER = getLogger(DefaultAlertingSupport.class);

  private static final Map<ClassLoader, AlertingSupport> ALERTS_PER_DEPLOYMENT = new WeakHashMap<>();

  private TimeSupplier timeSupplier;

  private Map<String, TimedDataBuffer> timedBuffersPerAlert = new ConcurrentHashMap<>();

  @Override
  public void initialise() throws InitialisationException {
    if (isMuleAlertingAsyncQueueFullPolicy()) {
      final ClassLoader tccl = currentThread().getContextClassLoader();
      final var regionContextClassLoader = artifactClassLoaderFinder().findRegionContextClassLoader().orElse(tccl);
      ALERTS_PER_DEPLOYMENT.put(regionContextClassLoader, this);
      register(regionContextClassLoader,
               regionClassLoader -> ALERTS_PER_DEPLOYMENT.get(regionClassLoader)
                   .triggerAlert(ALERT_ASYNC_LOGGER_RINGBUFFER_FULL));
    }

  }

  private boolean isMuleAlertingAsyncQueueFullPolicy() {
    final var callerLayer = this.getClass().getModule().getLayer();

    if (callerLayer != null) {
      return callerLayer
          // This will only be present on an actual Mule Runtime, not in unit tests
          .findModule("org.mule.runtime.logging")
          .isPresent();
    } else {
      // For test cases not running in a modularized way
      try {
        // This will only be present on an actual Mule Runtime, not in unit tests
        this.getClass().getClassLoader().loadClass("org.mule.runtime.module.log4j.api.MuleAlertingAsyncQueueFullPolicy");
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }
    }
  }

  private static Optional<ClassLoader> resolveRegionContextClassLoader(ClassLoader tccl) {
    return artifactClassLoaderFinder().findRegionContextClassLoader();
  }

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
                       e -> e.getValue().aggregate(baseIntevalAggregation, accumulator),
                       (x, y) -> x,
                       TreeMap::new));
  }

  @Inject
  public void setTimeSupplier(TimeSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

}
