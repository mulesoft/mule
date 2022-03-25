/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;
import org.mule.runtime.core.privileged.profiling.CoreProfilingService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link ReactiveProcessorEnricher} that implements the proactor pattern.
 *
 * @since 4.4.0
 */
public class ProactorProcessingStrategyEnricher implements ReactiveProcessorEnricher {

  private final int maxConcurrency;
  private final int parallelism;
  private final int subscribers;
  private final Supplier<Scheduler> contextSchedulerSupplier;
  private final Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator;
  private final CoreProfilingService profilingService;
  private final String artifactId;
  private final String artifactType;

  public ProactorProcessingStrategyEnricher(Supplier<Scheduler> contextSchedulerSupplier,
                                            Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator,
                                            CoreProfilingService profilingService,
                                            String artifactId,
                                            String artifactType,
                                            int maxConcurrency,
                                            int parallelism,
                                            int subscribers) {
    this.schedulerDecorator = schedulerDecorator;
    this.profilingService = profilingService;
    this.maxConcurrency = maxConcurrency;
    this.parallelism = parallelism;
    this.subscribers = subscribers;
    this.contextSchedulerSupplier = contextSchedulerSupplier;
    this.artifactId = artifactId;
    this.artifactType = artifactType;

  }

  @Override
  public ReactiveProcessor enrich(ReactiveProcessor processor) {
    return processingStrategyReactiveProcessorFrom(processor, contextSchedulerSupplier.get(), artifactId, artifactType)
        .withDispatcherScheduler(schedulerDecorator.apply(contextSchedulerSupplier.get()))
        .withProfilingService(profilingService)
        .withParallelism(getChainParallelism(processor))
        .build();
  }

  private int getChainParallelism(ReactiveProcessor processor) {
    // TODO MULE-19526: Technical debt: the resolution of the level of parallelism in proactor should be refactored
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

}
