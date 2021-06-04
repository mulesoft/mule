/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.provider.MuleManagementUtilsProvider;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;

/**
 * A {@link ProcessingStrategyEnricher} used for implementing the proactor pattern with CPU_INTENSIVE and IO_RW processing types.
 */
public class CpuIntensiveProcessingStrategyEnricher extends AbstractProactorProcessingStrategyEnricher {

  public CpuIntensiveProcessingStrategyEnricher(ProcessingStrategyEnricher nextCustomizer,
                                                MuleManagementUtilsProvider muleManagementUtilsProvider,
                                                Supplier<Scheduler> contextSchedulerSupplier,
                                                Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator,
                                                int maxConcurrency, int parallelism, int subscribers) {
    super(nextCustomizer, muleManagementUtilsProvider, contextSchedulerSupplier, schedulerDecorator, maxConcurrency,
          parallelism,
          subscribers);
  }

  @Override
  public Set<ReactiveProcessor.ProcessingType> getProcessingTypes() {
    return of(CPU_INTENSIVE);
  }
}
