/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.discovery;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;

import java.util.HashSet;
import java.util.Set;

public class CompositeProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

  Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies;

  public CompositeProfilingDataConsumerDiscoveryStrategy(Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies) {
    this.discoveryStrategies = discoveryStrategies;
  }

  @Override
  public Set<ProfilingDataConsumer<?>> discover() {
    Set<ProfilingDataConsumer<?>> profilingDataConsumers = new HashSet<>();
    discoveryStrategies.forEach(profilingDataConsumerDiscoveryStrategy -> profilingDataConsumers
        .addAll(profilingDataConsumerDiscoveryStrategy.discover()));
    return profilingDataConsumers;
  }
}
