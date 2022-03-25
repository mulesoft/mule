/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getLocation;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;
import org.reactivestreams.Publisher;
import org.mule.runtime.core.privileged.profiling.CoreProfilingService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Builder for a {@link ReactiveProcessor} that enriches a pipeline {@link ReactiveProcessor} with processing strategy logic. The
 * processing strategy involves a decision regarding
 * <p>
 * the {@link Scheduler} to use for dispatching the a {@link CoreEvent} to a flow.
 *
 * @since 4.4.0
 */
public class PipelineProcessingStrategyReactiveProcessorBuilder {

  private final String artifactId;
  private final String artifactType;
  private final ReactiveProcessor pipeline;
  private final ClassLoader executionClassloader;
  private ScheduledExecutorService scheduler;
  private CoreProfilingService profilingService;

  private PipelineProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor pipeline, ClassLoader executionClassloader,
                                                             String artifactId, String artifactType) {
    this.pipeline = pipeline;
    this.executionClassloader = executionClassloader;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
  }

  /**
   * @param pipeline             pipeline to which the processing strategy logic should be applied.
   * @param executionClassloader classloader used for pipeline execution.
   * @return the message processor chain builder
   */
  public static PipelineProcessingStrategyReactiveProcessorBuilder pipelineProcessingStrategyReactiveProcessorFrom(
                                                                                                                   ReactiveProcessor pipeline,
                                                                                                                   ClassLoader executionClassloader,
                                                                                                                   String artifactId,
                                                                                                                   String artifactType) {
    return new PipelineProcessingStrategyReactiveProcessorBuilder(pipeline, executionClassloader, artifactId, artifactType);
  }

  /**
   * @param scheduler the {@link Scheduler} used for dispatching events to the pipeline
   * @return the builder with {@link Scheduler}.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withScheduler(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
    return this;
  }

  /**
   * @param profilingService the profiling service used for profiling.
   * @return the builder with decorator set.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withProfilingService(
                                                                                 CoreProfilingService profilingService) {
    this.profilingService = profilingService;
    return this;
  }

  public ReactiveProcessor build() {
    return publisher -> baseProcessingStrategyPublisherBuilder(buildFlux(publisher)).build();
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> baseProcessingStrategyPublisherBuilder(
                                                                                                  ReactorPublisherBuilder<T> publisher) {

    ComponentLocation location = getLocation(pipeline);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> psSchedulingFlowExecutionDataProducer =
        dataProducerFromProfilingService(PS_SCHEDULING_FLOW_EXECUTION);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> startingFlowExecutionDataproducer =
        dataProducerFromProfilingService(STARTING_FLOW_EXECUTION);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> flowExecutedDataProducer =
        dataProducerFromProfilingService(FLOW_EXECUTED);

    Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transfomer =
        coreEvent -> new DefaultComponentProcessingStrategyProfilingEventContext(coreEvent, getLocation(pipeline),
                                                                                 Thread.currentThread().getName(), artifactId,
                                                                                 artifactType, currentTimeMillis());

    return publisher
        .profileProcessingStrategyEvent(profilingService, psSchedulingFlowExecutionDataProducer, transfomer)
        .publishOn(ofNullable(scheduler))
        .profileProcessingStrategyEvent(profilingService, startingFlowExecutionDataproducer, transfomer)
        .doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
        .transform(pipeline)
        .profileProcessingStrategyEvent(profilingService, flowExecutedDataProducer, transfomer);
  }

  private ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> dataProducerFromProfilingService(
                                                                                                                              ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType) {
    return profilingService.getProfilingDataProducer(profilingEventType);
  }
}


