/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.DefaultReactorSink;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.ReactorSink;

import org.reactivestreams.Publisher;

import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.EmitterProcessor;

/**
 * {@link Sink} implementation that dispatches incoming events directly to to the {@link Flow} serializing concurrent events.
 *
 * @since 4.0
 */
class DirectSink implements Sink, Disposable {

  private final ReactorSink reactorSink;

  /**
   * Create new {@link DirectSink}.
   *
   * @param function the processor to process events emitted onto stream, typically this processor will represent the flow
   *        pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public DirectSink(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> function,
                    Consumer<CoreEvent> eventConsumer, int bufferSize) {
    EmitterProcessor<CoreEvent> emitterProcessor = EmitterProcessor.create(bufferSize);
    reactorSink =
        new DefaultReactorSink(emitterProcessor.sink(), emitterProcessor.transform(function).doOnError(throwable -> {
        }).subscribe(), eventConsumer, bufferSize);
  }

  @Override
  public void accept(CoreEvent event) {
    reactorSink.accept(event);
  }

  @Override
  public boolean emit(CoreEvent event) {
    return reactorSink.emit(event);
  }

  @Override
  public void dispose() {
    reactorSink.dispose();
  }
}
