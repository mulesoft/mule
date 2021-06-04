/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.ExecutionStrategy;
import org.mule.runtime.core.internal.management.provider.MuleManagementUtilsProvider;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;
import org.mule.runtime.core.internal.processor.strategy.StreamExecutionStrategy;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyTransformerBuilder.buildProcessorChainFrom;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;

/**
 * A {@link ProcessingStrategyEnricher} for processing types that implements the proactor pattern.
 *
 * @since 4.4.0, 4.3.0
 */
public abstract class AbstractProactorProcessingStrategyEnricher
    extends AbstractProcessingStrategyEnricher {

  private final ProcessingStrategyEnricher nextCustomizer;
  private final int maxConcurrency;
  private final int parallelism;
  private final int subscribers;
  private final MuleManagementUtilsProvider managementUtilsProvider;
  private final Supplier<Scheduler> contextSchedulerSupplier;
  private final Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator;

  public AbstractProactorProcessingStrategyEnricher(ProcessingStrategyEnricher nextCustomizer,
                                                    MuleManagementUtilsProvider muleManagementUtilsProvider,
                                                    Supplier<Scheduler> contextSchedulerSupplier,
                                                    Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator,
                                                    int maxConcurrency,
                                                    int parallelism,
                                                    int subscribers) {
    this.nextCustomizer = nextCustomizer;
    this.managementUtilsProvider = muleManagementUtilsProvider;
    this.schedulerDecorator = schedulerDecorator;
    this.maxConcurrency = maxConcurrency;
    this.parallelism = parallelism;
    this.subscribers = subscribers;
    this.contextSchedulerSupplier = contextSchedulerSupplier;
  }

  @Override
  protected ReactiveProcessor doCreateProcessingStrategyChain(ReactiveProcessor processor) {
    return buildProcessorChainFrom(processor, contextSchedulerSupplier.get())
        .withExecutionProfiler(managementUtilsProvider.getProcessingStrategyExecutionProfiler(processor))
        .withExecutionOrchestrator(managementUtilsProvider
            .getExecutionOrchestrator(processor, getExecutionStrategy(processor)))
        .withParallelism(getChainParallelism(processor))
        .build();
  }

  @Override
  public Optional<ProcessingStrategyEnricher> nextCustomizer() {
    return ofNullable(nextCustomizer);
  }

  private int getChainParallelism(ReactiveProcessor processor) {
    // FlatMap is the way reactor has to do parallel processing. Since this proactor method is used for the processors that are
    // not CPU_LITE, parallelism is wanted when the processor is blocked to do IO or doing long CPU work.
    if (maxConcurrency == 1) {
      return 1;
    } else if (maxConcurrency == MAX_VALUE) {
      if (processor instanceof ComponentInnerProcessor && !((ComponentInnerProcessor) processor).isBlocking()) {
        // For a no concurrency limit non blocking processor, the java SDK already handles parallelism internally, so no need to
        // do that here.
        return 1;
      } else {
        // For no limit, pass through the no limit meaning to Reactor's flatMap
        return MAX_VALUE;
      }
    } else {
      // Otherwise, enforce the concurrency limit from the config,
      return max(maxConcurrency / (parallelism * subscribers), 1);
    }
  }

  private ExecutionStrategy getExecutionStrategy(ReactiveProcessor processor) {
    return new StreamExecutionStrategy(getDispatcherScheduler(processor), IMMEDIATE_SCHEDULER,
                                       contextSchedulerSupplier.get());
  }

  private ScheduledExecutorService getDispatcherScheduler(ReactiveProcessor processor) {
    return schedulerDecorator.apply(contextSchedulerSupplier.get());
  }

}
