/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.construct.BackPressureReason;
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

  private final ReactiveProcessor processor;
  private final Consumer<CoreEvent> eventConsumer;

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
        // Make inner chains behave correctly in the context of this mono
        .subscriberContext(ctx -> ctx.put(WITHIN_PROCESS_TO_APPLY, true))
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
  public BackPressureReason emit(CoreEvent event) {
    accept(event);
    return null;
  }
}
