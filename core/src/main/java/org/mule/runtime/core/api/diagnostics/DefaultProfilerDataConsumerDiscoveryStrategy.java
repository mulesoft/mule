/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import org.mule.runtime.core.api.diagnostics.consumer.LoggerComponentProcessingStrategyDataConsumer;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class DefaultProfilerDataConsumerDiscoveryStrategy
    implements ProfilerDataConsumerDiscoveryStrategy {

  @Override
  public <S extends ProfilingDataConsumer<T>, T extends ProfilingEventContext> Set<S> discover() {
    return (Set<S>) ImmutableSet.of(new LoggerComponentProcessingStrategyDataConsumer());
  }

}
