/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.discovery;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ProfilingDataConsumerDiscoveryStrategy} that aggregates multiple {@link ProfilingDataConsumerDiscoveryStrategy}
 * instances into a single one.
 *
 * @since 4.5.0
 */
public class CompositeProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

  /**
   * Discovery strategies whose sets of discovered {@link ProfilingDataConsumer} will be returned by {@link #discover()}.
   */
  private final Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies;

  /**
   * @param discoveryStrategies Discovery strategies whose sets of discovered {@link ProfilingDataConsumer} will be returned by
   *                            {@link #discover()}.
   */
  public CompositeProfilingDataConsumerDiscoveryStrategy(Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies) {
    this.discoveryStrategies = discoveryStrategies;
  }

  /**
   * @return The set of {@link ProfilingDataConsumer} instances discovered by all the {@link #discoveryStrategies}.
   */
  @Override
  public Set<ProfilingDataConsumer<?>> discover() {
    Set<ProfilingDataConsumer<?>> profilingDataConsumers = new HashSet<>();
    discoveryStrategies.forEach(profilingDataConsumerDiscoveryStrategy -> profilingDataConsumers
        .addAll(profilingDataConsumerDiscoveryStrategy.discover()));
    return profilingDataConsumers;
  }
}
