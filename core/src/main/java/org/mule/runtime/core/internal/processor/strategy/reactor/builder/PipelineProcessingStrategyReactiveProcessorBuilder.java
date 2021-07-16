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
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_END;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
import org.mule.runtime.core.api.diagnostics.ProfilingEventType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.processor.chain.HasLocation;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.UnaryOperator;

/**
 * Builder for a {@link ReactiveProcessor} that enriches a pipeline {@link ReactiveProcessor} with processing strategy logic. The
 * processing strategy involves a decision regarding
 * <p>
 * the {@link Scheduler} to use for dispatching the a {@link CoreEvent} to a flow.
 *
 * @since 4.4.0
 */
public class PipelineProcessingStrategyReactiveProcessorBuilder {

  public static final String UNKOWN_FLOW = "UNKOWN_FLOW";
  private final ReactiveProcessor pipeline;
  private final ClassLoader executionClassloader;
  private final MuleContext muleContext;
  private Optional<ScheduledExecutorService> scheduler = empty();
  private Optional<DiagnosticsService> diagnosticsService = empty();
  private UnaryOperator<ScheduledExecutorService> schedulerDecorator = UnaryOperator.identity();

  private PipelineProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor pipeline, ClassLoader executionClassloader,
                                                             MuleContext muleContext) {
    this.pipeline = pipeline;
    this.executionClassloader = executionClassloader;
    this.muleContext = muleContext;
  }

  /**
   * @param pipeline             pipeline to which the processing strategy logic should be applied.
   * @param executionClassloader classloader used for pipeline execution.
   * @return the message processor chain builder
   */
  public static PipelineProcessingStrategyReactiveProcessorBuilder pipelineProcessingStrategyReactiveProcessorFrom(
                                                                                                                   ReactiveProcessor pipeline,
                                                                                                                   ClassLoader executionClassloader,
                                                                                                                   MuleContext muleContext) {
    return new PipelineProcessingStrategyReactiveProcessorBuilder(pipeline, executionClassloader, muleContext);
  }

  /**
   * @param scheduler the {@link Scheduler} used for dispatching events to the pipeline
   * @return the builder with {@link Scheduler}.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withScheduler(Scheduler scheduler) {
    this.scheduler = ofNullable(scheduler);
    return this;
  }

  /**
   * @param schedulerDecorator the decorator for the scheduler
   * @return the builder with decorator set.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withSchedulerDecorator(
                                                                                   UnaryOperator<ScheduledExecutorService> schedulerDecorator) {
    this.schedulerDecorator = schedulerDecorator;
    return this;
  }

  /**
   * @param diagnosticsService the diagnostics service used for profiling
   * @return the builder with decorator set.
   */
  public PipelineProcessingStrategyReactiveProcessorBuilder withDiagnosticsService(
                                                                                   DiagnosticsService diagnosticsService) {
    this.diagnosticsService = ofNullable(diagnosticsService);
    return this;
  }

  public ReactiveProcessor build() {
    return publisher -> baseProcessingStrategyPublisherBuilder(buildFlux(publisher)).build();
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> baseProcessingStrategyPublisherBuilder(
                                                                                                  ReactorPublisherBuilder<T> publisher) {

    ComponentLocation location = getLocation(pipeline);
    Optional<ProfilingDataProducer> dataProducerFlowDispatch = dataProducerFromDiagnosticsService(PS_FLOW_DISPATCH);
    Optional<ProfilingDataProducer> dataProducerFlowEnd = dataProducerFromDiagnosticsService(PS_FLOW_END);
    String artifactId = muleContext.getConfiguration().getId();
    String artifactType = muleContext.getArtifactType().getAsString();

    publisher.profileEvent(location, dataProducerFlowDispatch, artifactId, artifactType);

    return scheduler
        .map(sch -> publisher.publishOn(schedulerDecorator.apply(sch)))
        .orElse(publisher)
        .profileEvent(location, dataProducerFlowEnd, artifactId, artifactType)
        .doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
        .transform(pipeline);
  }


  private ComponentLocation getLocation(ReactiveProcessor pipeline) {
    if (pipeline instanceof HasLocation) {
      return ((HasLocation) pipeline).resolveLocation();
    }

    return null;
  }


  private Optional<ProfilingDataProducer> dataProducerFromDiagnosticsService(ProfilingEventType profilingEventType) {
    return diagnosticsService.map(ds -> ds.getProfilingDataProducer(profilingEventType));
  }

}


