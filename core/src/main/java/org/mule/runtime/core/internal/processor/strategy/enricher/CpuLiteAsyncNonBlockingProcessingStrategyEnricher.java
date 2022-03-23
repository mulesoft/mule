/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.profiling.CoreProfilingService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * A {@link ReactiveProcessorEnricher} for CPU_LITE processing type.
 *
 * @since 4.4.0
 */
public class CpuLiteAsyncNonBlockingProcessingStrategyEnricher implements ReactiveProcessorEnricher {

  private final Supplier<Scheduler> liteSchedulerSupplier;
  private final Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier;
  private final CoreProfilingService profilingService;
  private final String artifactId;
  private final String artifactType;

  public CpuLiteAsyncNonBlockingProcessingStrategyEnricher(Supplier<Scheduler> liteSchedulerSupplier,
                                                           Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier,
                                                           CoreProfilingService profilingService,
                                                           String artifactId,
                                                           String artifactType) {
    this.liteSchedulerSupplier = liteSchedulerSupplier;
    this.nonBlockingSchedulerSupplier = nonBlockingSchedulerSupplier;
    this.profilingService = profilingService;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
  }

  @Override
  public ReactiveProcessor enrich(ReactiveProcessor processor) {
    return processingStrategyReactiveProcessorFrom(processor, liteSchedulerSupplier.get(), artifactId, artifactType)
        .withCallbackScheduler(nonBlockingSchedulerSupplier.get())
        .withProfilingService(profilingService)
        .build();
  }
}
