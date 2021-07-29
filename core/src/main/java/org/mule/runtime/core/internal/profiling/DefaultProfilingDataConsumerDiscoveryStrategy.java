/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import com.google.common.collect.ImmutableSet;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentProcessingStrategyDataConsumer;

import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Simple {@link ProfilingDataConsumerDiscoveryStrategy} that programmatically generates the data consumers.
 *
 * @since 4.4
 */
public class DefaultProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

  @Override
  public <S extends ProfilingDataConsumer<T>, T extends ProfilingEventContext> Set<S> discover() {
    return (Set<S>) ImmutableSet.of(new LoggerComponentProcessingStrategyDataConsumer());
  }

}
