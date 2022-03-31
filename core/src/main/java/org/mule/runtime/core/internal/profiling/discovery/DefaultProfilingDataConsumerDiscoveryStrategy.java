/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.discovery;

import static com.google.common.collect.ImmutableSet.of;

import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.LoggerByteBufferAllocationProfilingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.ComponentProcessingStrategyDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentThreadingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.TaskSchedulingLoggerDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.TransactionLoggerDataConsumer;

import java.util.Set;

/**
 * Simple {@link ProfilingDataConsumerDiscoveryStrategy} that programmatically generates the data consumers.
 *
 * @since 4.4
 */
public class DefaultProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

  private final InternalProfilingService profilingService;

  public DefaultProfilingDataConsumerDiscoveryStrategy(InternalProfilingService profilingService) {
    this.profilingService = profilingService;
  }

  @Override
  public Set<ProfilingDataConsumer<?>> discover() {
    return of(new LoggerByteBufferAllocationProfilingDataConsumer(),
              new ComponentProcessingStrategyDataConsumer(profilingService),
              new LoggerComponentThreadingDataConsumer(),
              new TransactionLoggerDataConsumer(),
              new TaskSchedulingLoggerDataConsumer());
  }

}
