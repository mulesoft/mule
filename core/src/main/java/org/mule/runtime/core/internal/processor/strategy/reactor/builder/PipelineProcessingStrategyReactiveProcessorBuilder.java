/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Builder for a {@link ReactiveProcessor} that enriches a pipeline {@link ReactiveProcessor} with processing strategy logic. The
 * processing strategy involves a decision regarding
 * <p>
 * the {@link Scheduler} to use for dispatching the a {@link CoreEvent} to a flow.
 * <p>
 *
 * @since 4.4.0
 */
public class PipelineProcessingStrategyReactiveProcessorBuilder {

  private final ReactiveProcessor pipeline;
  private final ClassLoader executionClassloader;
  private Optional<ScheduledExecutorService> scheduler = empty();
  private Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator = identity();

  private PipelineProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor pipeline, ClassLoader executionClassloader) {
    this.pipeline = pipeline;
    this.executionClassloader = executionClassloader;
  }

  /**
   * @param pipeline             pipeline to which the processing strategy logic should be applied.
   * @param executionClassloader classloader used for pipeline execution.
   * @return the message processor chain builder
   */
  public static PipelineProcessingStrategyReactiveProcessorBuilder buildPipelineProcessingStrategyTransformerFrom(ReactiveProcessor pipeline,
                                                                                                                  ClassLoader executionClassloader) {
    return new PipelineProcessingStrategyReactiveProcessorBuilder(pipeline, executionClassloader);
  }

  /**
   * @param scheduler the {@link Scheduler} used for dispatching events to the pipeline
   * 
   * @return the builder with {@link Scheduler}.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withScheduler(Scheduler scheduler) {
    this.scheduler = ofNullable(scheduler);
    return this;
  }

  /**
   * @param schedulerDecorator the decorator for the scheduler
   *
   * @return the builder with decorator set.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withSchedulerDecorator(Function<ScheduledExecutorService, ScheduledExecutorService> schedulerDecorator) {
    this.schedulerDecorator = schedulerDecorator;
    return this;
  }

  public ReactiveProcessor build() {
    return publisher -> basePipelinePublisher(buildFlux(publisher)).build();
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> basePipelinePublisher(ReactorPublisherBuilder<T> publisher) {
    return scheduler
        .map(sch -> publisher.publishOn(schedulerDecorator.apply(sch)))
        .orElse(publisher)
        .doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
        .transform(pipeline);
  }

}


