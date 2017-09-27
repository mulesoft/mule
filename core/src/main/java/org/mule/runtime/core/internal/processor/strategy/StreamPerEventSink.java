/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

/**
 * {@link Sink} implementation that uses a {@link Mono} for each request
 */
public class StreamPerEventSink implements Sink {

  private ReactiveProcessor processor;
  private Consumer<CoreEvent> eventConsumer;

  /**
   * Creates a {@link StreamPerEventSink}.
   *
   * @param processor the processor to process events emitted onto stream, typically this processor will represent the flow
   *        pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public StreamPerEventSink(ReactiveProcessor processor, Consumer<CoreEvent> eventConsumer) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
  }

  @Override
  public void accept(CoreEvent event) {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    just(event)
        .doOnNext(request -> eventConsumer.accept(request))
        .transform(processor)
        .subscribe(null, exception::set);
    if (exception.get() != null) {
      if (exception.get() instanceof RuntimeException) {
        throw (RuntimeException) exception.get();
      } else {
        throw new MuleRuntimeException(exception.get());
      }
    }
  }

  @Override
  public boolean emit(CoreEvent event) {
    accept(event);
    return true;
  }
}
