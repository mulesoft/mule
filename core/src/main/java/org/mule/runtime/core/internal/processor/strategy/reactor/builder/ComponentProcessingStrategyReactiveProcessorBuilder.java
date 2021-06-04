/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Builder for a {@link ReactiveProcessor} that enriches a component {@link ReactiveProcessor} with processing strategy logic. The
 * processing strategy involves two decisions:
 * <p>
 * If the component task should be submitted using a {@link Scheduler} or if it should be done in the same thread.
 * <p>
 * If the response should be submitted using a {@link Scheduler} for returning to the flow or further processing can be done in
 * the same thread.
 * <p>
 *
 * @since 4.4.0
 */
public class ComponentProcessingStrategyReactiveProcessorBuilder {

  private final ReactiveProcessor processor;
  private final Scheduler contextScheduler;
  private int parallelism = 1;
  private Optional<ScheduledExecutorService> dispatcherScheduler = empty();
  private Optional<ScheduledExecutorService> callbackScheduler = empty();

  public ComponentProcessingStrategyReactiveProcessorBuilder(ReactiveProcessor processor, Scheduler contextScheduler) {
    this.processor = processor;
    this.contextScheduler = contextScheduler;
  }

  /**
   * Factory method for the builder.
   *
   * @param processor        a {@link ReactiveProcessor} for enrichment with processing strategy logic.
   * @param contextScheduler the {@link Scheduler} used for tasks during the component processing.
   * 
   * @return the builder being created.
   */
  public static ComponentProcessingStrategyReactiveProcessorBuilder processingStrategyReactiveProcessorFrom(ReactiveProcessor processor,
                                                                                                            Scheduler contextScheduler) {
    return new ComponentProcessingStrategyReactiveProcessorBuilder(processor, contextScheduler);
  }

  /**
   * @param parallelism the level of parallelism needed in the built {@link ReactiveProcessor}
   * 
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withParallelism(int parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  /**
   * @param dispatcherScheduler {@link Scheduler} used for dispatching the event for the component processing.
   *
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withDispatcherScheduler(ScheduledExecutorService dispatcherScheduler) {
    this.dispatcherScheduler = ofNullable(dispatcherScheduler);
    return this;
  }

  /**
   * @param callbackScheduler {@link Scheduler} for dispatching the response.
   *
   * @return the builder being created.
   */
  public ComponentProcessingStrategyReactiveProcessorBuilder withCallbackScheduler(ScheduledExecutorService callbackScheduler) {
    this.callbackScheduler = ofNullable(callbackScheduler);
    return this;
  }

  public ReactiveProcessor build() {
    if (parallelism == 1) {
      return publisher -> baseProcessingStrategyBuilder(buildFlux(publisher)).build();
    } else {
      // FlatMap is the way reactor has to do parallel processing.
      return publisher -> Flux.from(publisher)
          .flatMap(e -> baseProcessingStrategyBuilder(ReactorPublisherBuilder.buildMono(e)).build(),
                   parallelism);
    }
  }

  private <T extends Publisher> ReactorPublisherBuilder<T> baseProcessingStrategyBuilder(ReactorPublisherBuilder<T> builder) {
    ReactorPublisherBuilder<T> beforeProcssor = dispatcherScheduler
        .map(sch -> builder.publishOn(sch))
        .orElse(builder)
        .transform(processor);

    return callbackScheduler
        .map(sch -> beforeProcssor.publishOn(sch))
        .orElse(beforeProcssor)
        .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, contextScheduler));
  }

}
