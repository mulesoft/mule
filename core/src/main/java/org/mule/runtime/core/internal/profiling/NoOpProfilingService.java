/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * A Profiling Service that disables all data production. The {@link ProfilingDataProducer} implements operations that do not
 * propagate the profiling data.
 *
 * @since 4.5.0
 */
public class NoOpProfilingService implements CoreProfilingService {

  @SuppressWarnings("rawtypes")
  private final ProfilingDataProducer<?, ?> profilingDataProducer = new ProfilingDataProducer() {


    @Override
    public void triggerProfilingEvent(ProfilingEventContext profilerEventContext) {
      // No op
    }

    @Override
    public void triggerProfilingEvent(Object sourceData, Function transformation) {
      // No op
    }
  };

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType,
                                                                                                   ProfilingProducerScope producerContext) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @Override
  public <T extends ProfilingEventContext, S> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                                 ProfilingDataProducer<T, S> profilingDataProducer) {
    // Nothing to do
  }

  @Override
  public ThreadSnapshotCollector getThreadSnapshotCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends ProfilingEventContext, S> Mono<S> enrichWithProfilingEventMono(Mono<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original;
  }

  @Override
  public <T extends ProfilingEventContext, S> Flux<S> enrichWithProfilingEventFlux(Flux<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original;
  }
}
