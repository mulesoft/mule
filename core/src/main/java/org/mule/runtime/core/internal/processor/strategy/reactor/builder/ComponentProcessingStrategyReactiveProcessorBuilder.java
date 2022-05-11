/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getLocation;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import org.mule.runtime.core.internal.profiling.tracing.DefaultComponentMetadata;
import org.mule.runtime.core.internal.profiling.tracing.DefaultExecutionContext;
import reactor.core.publisher.Flux;
import org.reactivestreams.Publisher;

/**
 * Builder for a {@link ReactiveProcessor} that enriches a component {@link ReactiveProcessor} with processing strategy logic. The
 * processing strategy involves two decisions:
 * <p>
 * If the component task should be submitted using a {@link Scheduler} or if it should be done in the same thread.
 * <p>
 * If the response should be submitted using a {@link Scheduler} for returning to the flow or further processing can be done in
 * the same thread.
 *
 * @since 4.4.0
 */
public class ComponentProcessingStrategyReactiveProcessorBuilder {

  private final ReactiveProcessor processor;
  private final Scheduler contextScheduler;
  private final String artifactId;
  private final String artifactType;
  private int parallelism = 1;
  private ScheduledExecutorService dispatcherScheduler;
  private ScheduledExecutorService callbackScheduler;
  private InternalProfilingService profilingService;

  public ComponentProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor processor, Scheduler contextScheduler,
                                                             String artifactId, String artifactType) {
    this.processor = processor;
    this.contextScheduler = contextScheduler;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
  }

  /**
   * Factory method for the builder.
   *
   * @param processor        a {@link ReactiveProcessor} for enrichment with processing strategy logic.
   * @param contextScheduler the {@link Scheduler} used for tasks during the component processing.
   * @param artifactId       the artifact id of the artifact corresponding to the component.
   * @param artifactType     the artifact type of the artifact corresponding to the component.
   * @return the builder being created.
   */
  public static ComponentProcessingStrategyReactiveProcessorBuilder processingStrategyReactiveProcessorFrom(ReactiveProcessor processor,
                                                                                                            Scheduler contextScheduler,
                                                                                                            String artifactId,
                                                                                                            String artifactType) {
    return new ComponentProcessingStrategyReactiveProcessorBuilder(processor, contextScheduler, artifactId, artifactType);
  }

  /**
   * @param parallelism the level of parallelism needed in the built {@link ReactiveProcessor}
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withParallelism(int parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  /**
   * @param dispatcherScheduler {@link Scheduler} used for dispatching the event for the component processing.
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withDispatcherScheduler(
                                                                                     ScheduledExecutorService dispatcherScheduler) {
    this.dispatcherScheduler = dispatcherScheduler;
    return this;
  }

  /**
   * @param callbackScheduler {@link Scheduler} for dispatching the response.
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withCallbackScheduler(ScheduledExecutorService callbackScheduler) {
    this.callbackScheduler = callbackScheduler;
    return this;
  }

  /**
   * @param profilingService {@link ProfilingService} for profiling processing strategy logic.
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withProfilingService(InternalProfilingService profilingService) {
    this.profilingService = profilingService;
    return this;
  }

  public ReactiveProcessor build() {
    if (parallelism == 1) {
      return publisher -> baseProcessingStrategyPublisherBuilder(buildFlux(publisher)).build();
    } else {
      // FlatMap is the way reactor has to do parallel processing.
      return publisher -> Flux.from(publisher)
          .flatMap(e -> baseProcessingStrategyPublisherBuilder(ReactorPublisherBuilder.buildMono(e)).build(),
                   parallelism);
    }
  }

  private <T extends Publisher<?>> ReactorPublisherBuilder<T> baseProcessingStrategyPublisherBuilder(
                                                                                                     ReactorPublisherBuilder<T> builder) {

    // Profiling data producers
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> psSchedulingOperationExecutionDataProducer =
        profilingService.getProfilingDataProducer(PS_SCHEDULING_OPERATION_EXECUTION);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> startingOperationExecutionDataProducer =
        profilingService.getProfilingDataProducer(PS_STARTING_OPERATION_EXECUTION);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> operationExecutionDataProducer =
        profilingService.getProfilingDataProducer(PS_OPERATION_EXECUTED);
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> psFlowMessagePassingDataProducer =
        profilingService.getProfilingDataProducer(PS_FLOW_MESSAGE_PASSING);

    // Location
    ComponentLocation location = getLocation(processor);

    Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transformer =
        coreEvent -> new DefaultComponentProcessingStrategyProfilingEventContext(coreEvent, location,
                                                                                 Thread.currentThread().getName(), artifactId,
                                                                                 artifactType, currentTimeMillis());

    // General structure of processing strategy publishOn -> operation -> publishOn
    return builder
        .setTracingContext(profilingService,
                           coreEvent -> new DefaultExecutionContext(new DefaultComponentMetadata(coreEvent.getCorrelationId(),
                                                                                                 artifactId, artifactType,
                                                                                                 location)))
        .profileProcessingStrategyEvent(profilingService, psSchedulingOperationExecutionDataProducer, transformer)
        .publishOn(ofNullable(dispatcherScheduler))
        .profileProcessingStrategyEvent(profilingService, startingOperationExecutionDataProducer, transformer)
        .transform(processor)
        .profileProcessingStrategyEvent(profilingService, operationExecutionDataProducer, transformer)
        .publishOn(ofNullable(callbackScheduler))
        .profileProcessingStrategyEvent(profilingService, psFlowMessagePassingDataProducer, transformer)
        .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, contextScheduler));
  }

}
