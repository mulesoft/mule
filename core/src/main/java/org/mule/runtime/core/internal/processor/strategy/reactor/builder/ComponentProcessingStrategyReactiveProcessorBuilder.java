/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
import org.mule.runtime.core.api.diagnostics.ProfilingEventType;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import reactor.core.publisher.Flux;

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
  private int parallelism = 1;
  private MuleContext muleContext;
  private Optional<ScheduledExecutorService> dispatcherScheduler = empty();
  private Optional<ScheduledExecutorService> callbackScheduler = empty();
  private Optional<DiagnosticsService> diagnosticsService = empty();

  public ComponentProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor processor, Scheduler contextScheduler,
                                                             MuleContext muleContext) {
    this.processor = processor;
    this.contextScheduler = contextScheduler;
    this.muleContext = muleContext;
  }

  /**
   * Factory method for the builder.
   *
   * @param processor        a {@link ReactiveProcessor} for enrichment with processing strategy logic.
   * @param contextScheduler the {@link Scheduler} used for tasks during the component processing.
   * @return the builder being created.
   */
  public static ComponentProcessingStrategyReactiveProcessorBuilder processingStrategyReactiveProcessorFrom(
                                                                                                            ReactiveProcessor processor,
                                                                                                            Scheduler contextScheduler,
                                                                                                            MuleContext muleContext) {
    return new ComponentProcessingStrategyReactiveProcessorBuilder(processor, contextScheduler, muleContext);
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
    this.dispatcherScheduler = ofNullable(dispatcherScheduler);
    return this;
  }

  /**
   * @param callbackScheduler {@link Scheduler} for dispatching the response.
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withCallbackScheduler(ScheduledExecutorService callbackScheduler) {
    this.callbackScheduler = ofNullable(callbackScheduler);
    return this;
  }

  /**
   * @param diagnosticsService {@link DiagnosticsService} for profiling processing strategy logic.
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withDiagnosticsService(DiagnosticsService diagnosticsService) {
    this.diagnosticsService = ofNullable(diagnosticsService);
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

  private <T extends Publisher> ReactorPublisherBuilder<T> baseProcessingStrategyPublisherBuilder(
                                                                                                  ReactorPublisherBuilder<T> builder) {

    // Profiling hooks
    Optional<ProfilingDataProducer> dispatchingOperationExecutionHook =
        hookFromDiagnosticsService(PS_SCHEDULING_OPERATION_EXECUTION);
    Optional<ProfilingDataProducer> operationExecutionDispatchedHook = hookFromDiagnosticsService(STARTING_OPERATION_EXECUTION);
    Optional<ProfilingDataProducer> dispatchingOperationResultHook = hookFromDiagnosticsService(OPERATION_EXECUTED);
    Optional<ProfilingDataProducer> operationResultDispatchedHook = hookFromDiagnosticsService(PS_FLOW_MESSAGE_PASSING);

    // location
    ComponentLocation location = getLocation(processor);

    // Add the reactor processor enrichment with the processing strategy scheduling before the processor transform.
    ReactorPublisherBuilder<T> beforeProcessor =
        getBeforeProcessorReactorChain(builder, dispatchingOperationExecutionHook, operationExecutionDispatchedHook, location,
                                       muleContext.getConfiguration().getId(), muleContext.getArtifactType().getAsString());

    // Add the reactor processing enrichment with the processing strategy scheduling after the processor transform.
    return getAfterProcessorReactorChain(dispatchingOperationResultHook, operationResultDispatchedHook, location,
                                         muleContext.getConfiguration().getId(),
                                         muleContext.getArtifactType().getAsString(),
                                         beforeProcessor);
  }

  private Optional<ProfilingDataProducer> hookFromDiagnosticsService(ProfilingEventType profilingEventType) {
    return diagnosticsService.map(ds -> of(ds.getProfilingDataProducer(profilingEventType))).orElse(empty());
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> getAfterProcessorReactorChain(
                                                                                         Optional<ProfilingDataProducer> dispatchingOperationResultHook,
                                                                                         Optional<ProfilingDataProducer> operationResultDispatchedHook,
                                                                                         ComponentLocation location,
                                                                                         String artifactId,
                                                                                         String artifactType,
                                                                                         ReactorPublisherBuilder<T> beforeProcessor) {
    return callbackScheduler
        .map(sch -> beforeProcessor
            .profileEvent(location, dispatchingOperationResultHook, artifactId, artifactType)
            .publishOn(sch)
            .profileEvent(location, operationResultDispatchedHook, artifactId, artifactType))
        .orElse(beforeProcessor
            .profileEvent(location, dispatchingOperationResultHook, artifactId, artifactType)
            .profileEvent(location, operationResultDispatchedHook, artifactId, artifactType))
        .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, contextScheduler));
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> getBeforeProcessorReactorChain(ReactorPublisherBuilder<T> builder,
                                                                                          Optional<ProfilingDataProducer> dispatchingOperationExecutionHook,
                                                                                          Optional<ProfilingDataProducer> operationExecutionDispatchedHook,
                                                                                          ComponentLocation location,
                                                                                          String artifactId,
                                                                                          String artifactType) {
    return dispatcherScheduler
        .map(sch -> builder
            .profileEvent(location, dispatchingOperationExecutionHook, artifactId, artifactType)
            .publishOn(sch)
            .profileEvent(location, operationExecutionDispatchedHook, artifactId, artifactType))
        .orElse(builder
            .profileEvent(location, dispatchingOperationExecutionHook, artifactId, artifactType)
            .profileEvent(location, operationExecutionDispatchedHook, artifactId, artifactType)
            .transform(processor));
  }

  private ComponentLocation getLocation(ReactiveProcessor processor) {
    if (processor instanceof InterceptedReactiveProcessor) {
      return getLocation(((InterceptedReactiveProcessor) processor).getProcessor());
    }

    if (processor instanceof Component) {
      return ((Component) processor).getLocation();
    }

    if (processor instanceof ComponentInnerProcessor) {
      return ((ComponentInnerProcessor) processor).getLocation();
    }

    return null;
  }
}
