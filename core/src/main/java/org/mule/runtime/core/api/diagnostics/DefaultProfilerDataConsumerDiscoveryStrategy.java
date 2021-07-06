/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import com.google.common.collect.ImmutableSet;
import org.mule.runtime.core.api.diagnostics.consumer.LoggerComponentProcessingStrategyDataConsumer;

import java.util.Set;

public class DefaultProfilerDataConsumerDiscoveryStrategy
    implements ProfilerDataConsumerDiscoveryStrategy {

  @Override
  public Set<ProfilingDataConsumer> discover() {
    // Add default profiler data consumers by the mule
    return ImmutableSet.of(new LoggerComponentProcessingStrategyDataConsumer());
  }
}
