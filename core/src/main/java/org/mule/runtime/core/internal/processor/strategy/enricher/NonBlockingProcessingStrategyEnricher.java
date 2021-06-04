/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.ExecutionStrategy;
import org.mule.runtime.core.internal.management.provider.MuleManagementUtilsProvider;
import org.mule.runtime.core.internal.processor.strategy.StreamExecutionStrategy;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyTransformerBuilder.buildProcessorChainFrom;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;

/**
 * An abstract {@link ProcessingStrategyEnricher} for non blocking processing.
 *
 * The processor will be processed in the same thread
 * 
 * @since 4.4.0, 4.3.1
 */
public abstract class NonBlockingProcessingStrategyEnricher extends AbstractProcessingStrategyEnricher {

  protected final Supplier<Scheduler> liteSchedulerProvider;
  protected final Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier;
  private ProcessingStrategyEnricher nextCustomizer;
  private MuleManagementUtilsProvider managementUtilsProvider;

  public NonBlockingProcessingStrategyEnricher(ProcessingStrategyEnricher nextCustomizer,
                                               MuleManagementUtilsProvider managementUtilsProvider,
                                               Supplier<Scheduler> liteSchedulerProvider,
                                               Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier) {
    this.nextCustomizer = nextCustomizer;
    this.managementUtilsProvider = managementUtilsProvider;
    this.liteSchedulerProvider = liteSchedulerProvider;
    this.nonBlockingSchedulerSupplier = nonBlockingSchedulerSupplier;
  }

  @Override
  public Optional<ProcessingStrategyEnricher> nextCustomizer() {
    return ofNullable(nextCustomizer);
  }


  @Override
  protected ReactiveProcessor doCreateProcessingStrategyChain(ReactiveProcessor processor) {
    return buildProcessorChainFrom(processor, liteSchedulerProvider.get())
        .withExecutionOrchestrator(managementUtilsProvider
            .getExecutionOrchestrator(processor, getExecutionStrategy()))
        .withExecutionProfiler(managementUtilsProvider.getProcessingStrategyExecutionProfiler(processor))
        .build();
  }

  /**
   * @return the {@link ExecutionStrategy} that will provide the {@link Scheduler}'s to be used when dispatching a
   *         {@link CoreEvent}.
   */
  protected ExecutionStrategy getExecutionStrategy() {
    return new StreamExecutionStrategy(IMMEDIATE_SCHEDULER, nonBlockingSchedulerSupplier.get(), liteSchedulerProvider.get());
  }

}
