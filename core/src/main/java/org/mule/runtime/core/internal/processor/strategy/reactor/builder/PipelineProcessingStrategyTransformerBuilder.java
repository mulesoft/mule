/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.pipeline.DefaultProcessingStrategyPipelineProfiler;
import org.mule.runtime.core.internal.management.pipeline.ProcessingStrategyPipelineProfiler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.lang.Thread.currentThread;
import static java.util.function.Function.identity;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

/**
 * Builder for a transformer that involves creation of a reactor chain with a processing strategy applied for the pipeline. The
 * processing strategy involves decisions concerning:
 * <p>
 * The {@link Scheduler} used to dispatch the event to the flow.
 * <p>
 *
 * @since 4.4.0, 4.3.1
 */
public class PipelineProcessingStrategyTransformerBuilder {

  private final ReactiveProcessor pipeline;
  private final ClassLoader executionClassloader;
  private ScheduledExecutorService scheduler = IMMEDIATE_SCHEDULER;
  private Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator = identity();
  private ProcessingStrategyPipelineProfiler profiler = new DefaultProcessingStrategyPipelineProfiler();

  private PipelineProcessingStrategyTransformerBuilder(ReactiveProcessor pipeline, ClassLoader executionClassloader) {
    this.pipeline = pipeline;
    this.executionClassloader = executionClassloader;
  }

  /**
   * Factory method for the builder.
   *
   * @param pipeline             pipeline from which the processing strategy should be created.
   * @param executionClassloader classloader used for pipeline execution.
   * @return the message processor chain builder
   */
  public static PipelineProcessingStrategyTransformerBuilder buildPipelineProcessingStrategyTransformerFrom(ReactiveProcessor pipeline,
                                                                                                            ClassLoader executionClassloader) {
    return new PipelineProcessingStrategyTransformerBuilder(pipeline, executionClassloader);
  }

  /**
   * @param scheduler the {@link Scheduler} used for dispatching events to the pipeline
   * 
   * @return the builder with {@link Scheduler}.
   */
  public PipelineProcessingStrategyTransformerBuilder withScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
    return this;
  }

  /**
   * @param schedulerDecorator the decorator for the scheduler
   *
   * @return the builder with decorator set.
   */
  public PipelineProcessingStrategyTransformerBuilder withSchedulerDecorator(Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator) {
    this.schedulerDecorator = schedulerDecorator;
    return this;
  }

  /**
   * @param @{link PipelineProcessingStrategyTransformerBuilder} profiler for the actions involved in the pipeline dispatching
   * 
   * @return the builder with the profiler set.
   */
  public PipelineProcessingStrategyTransformerBuilder withProfiler(ProcessingStrategyPipelineProfiler profiler) {
    this.profiler = profiler;
    return this;
  }

  public ReactiveProcessor build() {
    return publisher -> from(publisher)
        .doOnNext(e -> profiler.profileBeforeDispatchingToPipeline(e))
        .publishOn(fromExecutorService(schedulerDecorator.apply(scheduler)))
        .doOnNext(e -> profiler.profileAfterPipelineProcessed(e))
        .doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
        .transform(pipeline);
  }

}


