/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.tracing.ExecutionContext;

import java.util.function.Function;

import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A Profiling Service that adds some extra internal functionality related to reactor. This is used only by the runtime core.
 *
 * @since 4.5.0
 */
public interface InternalProfilingService extends ProfilingService {

  /**
   * Enriches {@link Mono} with profiling event.
   *
   * @param original     the original {@link Mono}
   * @param dataProducer the {@link ProfilingDataProducer} used for profiling
   * @param transformer  the transformer function
   * @param <T>          Generic Type for {@link ProfilingDataProducer}
   * @param <S>          Generic Type for {@link ProfilingDataProducer}
   *
   * @return the enriched {@link Mono}
   */
  <T extends ProfilingEventContext, S> Mono<S> enrichWithProfilingEventMono(Mono<S> original,
                                                                            ProfilingDataProducer<T, S> dataProducer,
                                                                            Function<S, T> transformer);

  /**
   * Enriches {@link Flux} with profiling event.
   *
   * @param original     the original {@link Flux}
   * @param dataProducer the {@link ProfilingDataProducer} used for profiling
   * @param transformer  the transformer function
   * @param <T>          Generic Type for {@link ProfilingDataProducer}
   * @param <S>          Generic Type for {@link ProfilingDataProducer}
   *
   * @return the enriched {@link Flux}
   */
  <T extends ProfilingEventContext, S> Flux<S> enrichWithProfilingEventFlux(Flux<S> original,
                                                                            ProfilingDataProducer<T, S> dataProducer,
                                                                            Function<S, T> transformer);

  <S> Mono<S> setCurrentExecutionContext(Mono<S> original, Function<S, ExecutionContext> executionContextSupplier);

  <S> Flux<S> setCurrentExecutionContext(Flux<S> original, Function<S, ExecutionContext> executionContextSupplier);

  /**
   * @return the {@link CoreEventTracer}.
   *
   * @since 4.5.0
   */
  CoreEventTracer getCoreEventTracer();
}
