/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static reactor.core.publisher.DirectProcessor.create;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Sink;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;

/**
 * {@link Sink} implementation that dispatches incoming events directly to to the {@link Flow} serializing concurrent events.
 *
 * @since 4.0
 */
class DirectSink implements Sink, Disposable {

  private AbstractProcessingStrategy.ReactorSink reactorSink;

  /**
   * Create new {@link DirectSink}.
   *
   * @param function the processor to process events emitted onto stream, typically this processor will represent the flow
   *        pipeline.
   * @param eventConsumer event consumer called just before {@link Event}'s emission.
   */
  public DirectSink(Function<Publisher<Event>, Publisher<Event>> function, Consumer<Event> eventConsumer) {
    DirectProcessor<Event> directProcessor = create();
    reactorSink =
        new AbstractProcessingStrategy.ReactorSink(directProcessor.serialize().connectSink(),
                                                   directProcessor.transform(function).doOnError(throwable -> {
                                                   }).subscribe(),
                                                   eventConsumer);
  }

  @Override
  public void accept(Event event) {
    reactorSink.accept(event);
  }

  @Override
  public void dispose() {
    reactorSink.dispose();
  }
}
