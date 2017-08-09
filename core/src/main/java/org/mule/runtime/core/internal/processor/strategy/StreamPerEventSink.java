/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * {@link Sink} implementation that uses a {@link Mono} for each request
 */
public class StreamPerEventSink implements Sink {

  private ReactiveProcessor processor;
  private Consumer<Event> eventConsumer;

  /**
   * Creates a {@link StreamPerEventSink}.
   *
   * @param processor the processor to process events emitted onto stream, typically this processor will represent the flow
   *        pipeline.
   * @param eventConsumer event consumer called just before {@link Event}'s emission.
   */
  public StreamPerEventSink(ReactiveProcessor processor, Consumer<Event> eventConsumer) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
  }

  @Override
  public void accept(Event event) throws RejectedExecutionException {
    eventConsumer.accept(event);
    AtomicReference<Throwable> rejected = new AtomicReference();
    just(event)
        .transform(processor)
        .doOnError(RejectedExecutionException.class, rejected::set)
        .subscribe(requestUnbounded());
    if (rejected.get() instanceof RejectedExecutionException) {
      throw (RejectedExecutionException) rejected.get();
    }
  }
}
