/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.tracing.ExecutionContext;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Builder of a {@link Publisher} with common operations in {@link Mono} and {@link Flux}.
 *
 * @param <T>
 * @since 4.4.0
 */
public interface ReactorPublisherBuilder<T extends Publisher> {

  /**
   * @param event the {@link CoreEvent} to emit.
   * @return a builder for a {@link Mono} that just emits one event.
   */
  static ReactorPublisherBuilder<Mono<CoreEvent>> buildMono(CoreEvent event) {
    return new MonoBuilder(Mono.just(event));
  }

  /**
   * @param publisher the {@link Publisher} used as a base for creating the {@link Flux}.
   * @return a builder for a {@link Flux} that emits the same events from the publisher.
   */
  static ReactorPublisherBuilder<Flux<CoreEvent>> buildFlux(Publisher<CoreEvent> publisher) {
    return new FluxBuilder(Flux.from(publisher));
  }

  /**
   * @param onNext what to do on next.
   * @return builder with a doOnNext operator.
   */
  ReactorPublisherBuilder<T> doOnNext(Consumer<CoreEvent> onNext);

  /**
   * @param scheduler optional scheduler to publish on.
   * @return builder which publishes on the scheduler.
   */
  ReactorPublisherBuilder<T> publishOn(Optional<ScheduledExecutorService> scheduler);

  /**
   * @param processor a {@link ReactiveProcessor} to transform the {@link Publisher}.
   * @return builder for the transformed {@link Publisher}.
   */
  ReactorPublisherBuilder<T> transform(ReactiveProcessor processor);

  /**
   * @param function the function that sets the conte?t on subscription.
   * @return builder for a {@link Publisher} with the subscriberContext set.
   */
  ReactorPublisherBuilder<T> subscriberContext(Function<Context, Context> function);

  /**
   * @param onSubscribe operation done onSubscribe
   * @return builder for a {@link Publisher} with the onSubscribe performed.
   */
  ReactorPublisherBuilder<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe);

  ReactorPublisherBuilder<T> profileProcessingStrategyEvent(InternalProfilingService profilingService,
                                                            ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> dataProducer,
                                                            Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transformer);

  ReactorPublisherBuilder<T> setTracingContext(InternalProfilingService profilingService,
                                               Function<CoreEvent, ExecutionContext> executionContextSupplier);

  T build();

  /**
   * Builder for a {@link Mono}.
   */
  class MonoBuilder implements ReactorPublisherBuilder<Mono<CoreEvent>> {

    Mono<CoreEvent> mono;

    private MonoBuilder(Mono<CoreEvent> mono) {
      this.mono = mono;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> doOnNext(Consumer<CoreEvent> onNext) {
      mono = mono.doOnNext(onNext);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> publishOn(Optional<ScheduledExecutorService> scheduler) {
      mono = scheduler.map(sch -> mono.publishOn(fromExecutorService(sch))).orElse(mono);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> transform(ReactiveProcessor processor) {
      mono = mono.transform(processor);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> subscriberContext(Function<Context, Context> function) {
      mono = mono.contextWrite(function);
      return this;
    }

    @Override
    public Mono<CoreEvent> build() {
      return mono;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
      mono = mono.doOnSubscribe(onSubscribe);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> profileProcessingStrategyEvent(InternalProfilingService profilingService,
                                                                                   ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> dataProducer,
                                                                                   Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transformer) {
      mono = profilingService.enrichWithProfilingEventMono(mono, dataProducer, transformer);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Mono<CoreEvent>> setTracingContext(InternalProfilingService profilingService,
                                                                      Function<CoreEvent, ExecutionContext> executionContextSupplier) {
      mono = profilingService.setCurrentExecutionContext(mono, executionContextSupplier);
      return this;
    }

  }


  /**
   * Builder for a {@link Flux}.
   */
  class FluxBuilder implements ReactorPublisherBuilder<Flux<CoreEvent>> {

    Flux<CoreEvent> flux;

    private FluxBuilder(Flux<CoreEvent> flux) {
      this.flux = flux;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> doOnNext(Consumer<CoreEvent> onNext) {
      flux = flux.doOnNext(onNext);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> publishOn(Optional<ScheduledExecutorService> scheduler) {
      flux = scheduler.map(sch -> flux.publishOn(fromExecutorService(sch))).orElse(flux);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> transform(ReactiveProcessor processor) {
      flux = flux.transform(processor);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> subscriberContext(Function<Context, Context> function) {
      flux = flux.contextWrite(function);
      return this;
    }

    @Override
    public Flux<CoreEvent> build() {
      return flux;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
      flux = flux.doOnSubscribe(onSubscribe);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> profileProcessingStrategyEvent(InternalProfilingService profilingService,
                                                                                   ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> dataProducer,
                                                                                   Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transformer) {
      flux = profilingService.enrichWithProfilingEventFlux(flux, dataProducer, transformer);
      return this;
    }

    @Override
    public ReactorPublisherBuilder<Flux<CoreEvent>> setTracingContext(InternalProfilingService profilingService,
                                                                      Function<CoreEvent, ExecutionContext> executionContextSupplier) {
      flux = profilingService.setCurrentExecutionContext(flux, executionContextSupplier);
      return this;
    }

  }
}
