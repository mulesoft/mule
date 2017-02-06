/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Abstract processing strategy that, rather than using a single shared stream, creates a stream per {@link Event}.
 */
abstract class AbstractStreamPerEventProcessingStrategyFactory extends AbstractProcessingStrategy {

  @Override
  public final Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
    return new Sink() {

      Consumer<Event> onEventConsumer = createOnEventConsumer();

      @Override
      public void accept(Event event) {
        onEventConsumer.accept(event);
        just(event).transform(function).subscribe();
      }
    };
  }

}
