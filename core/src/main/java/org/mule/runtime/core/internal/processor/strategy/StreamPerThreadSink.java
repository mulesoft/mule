/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.yield;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link Sink} implementation that uses a {@link Flux} for each thread that dispatches events to it.
 */
public class StreamPerThreadSink implements Sink, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamPerThreadSink.class);

  private final ReactiveProcessor processor;
  private final Consumer<CoreEvent> eventConsumer;
  private final FlowConstruct flowConstruct;

  private volatile boolean disposing = false;
  private final Cache<Thread, FluxSink<CoreEvent>> sinks = Caffeine.newBuilder().weakKeys().build();

  /**
   * Creates a {@link StreamPerThreadSink}.
   *
   * @param processor the processor to process events emitted onto stream, typically this processor will represent the flow
   *        pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public StreamPerThreadSink(ReactiveProcessor processor, Consumer<CoreEvent> eventConsumer, FlowConstruct flowConstruct) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
    this.flowConstruct = flowConstruct;
  }

  @Override
  public void accept(CoreEvent event) {
    if (disposing) {
      throw new IllegalStateException("Already disposed");
    }

    sinks.get(currentThread(), t -> {
      final FluxSinkRecorder<CoreEvent> recorder = new FluxSinkRecorder<>();
      Flux.create(recorder)
          .doOnNext(request -> eventConsumer.accept(request))
          .transform(processor)
          .subscribe(null, e -> sinks.invalidate(currentThread()), () -> sinks.invalidate(currentThread()));

      return recorder.getFluxSink();
    })
        .next(event);
  }

  @Override
  public boolean emit(CoreEvent event) {
    accept(event);
    return true;
  }

  @Override
  public void dispose() {
    disposing = true;
    sinks.asMap().values().forEach(sink -> sink.complete());

    final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
    long startMillis = currentTimeMillis();

    while (!sinks.asMap().isEmpty()
        && currentTimeMillis() - startMillis > shutdownTimeout
        && !currentThread().isInterrupted()) {
      yield();
    }

    if (currentThread().isInterrupted()) {
      LOGGER.warn("Subscribers of ProcessingStrategy for flow '{}' not completed before thread interruption",
                  flowConstruct.getName());
      return;
    }

    if (!sinks.asMap().isEmpty()) {
      LOGGER.warn("Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                  shutdownTimeout);
      sinks.invalidateAll();
    }
  }
}
