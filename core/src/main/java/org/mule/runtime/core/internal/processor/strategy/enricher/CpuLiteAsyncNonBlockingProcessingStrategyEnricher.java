/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy.enricher;

import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

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
  private final InternalProfilingService profilingService;
  private final String artifactId;
  private final String artifactType;

  public CpuLiteAsyncNonBlockingProcessingStrategyEnricher(Supplier<Scheduler> liteSchedulerSupplier,
                                                           Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier,
                                                           InternalProfilingService profilingService,
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
