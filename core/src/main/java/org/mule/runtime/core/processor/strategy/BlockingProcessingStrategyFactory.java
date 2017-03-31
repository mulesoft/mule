/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.rx.Exceptions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processing strategy that processes the {@link Pipeline} in the caller thread and does not schedule the processing of any
 * {@link Processor} in a different thread pool regardless of their {@link ReactiveProcessor.ProcessingType}. When individual
 * {@link Processor}'s may however execute non-blocking operations, e.g. an outbound HTTP request, the {@link Pipeline} will block
 * until the operation response is available before continuing processing in the same thread. This will not force the use of a
 * single thread, as internally operations may still leverage non-blocking, but it does ensure the {@link Pipeline} itself is
 * executed in a blocking manner using a single caller thread,
 */

public class BlockingProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy BLOCKING_PROCESSING_STRATEGY_INSTANCE =
      new AbstractProcessingStrategy() {

        @Override
        public boolean isSynchronous() {
          return true;
        }

        @Override
        public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
          return new StreamPerEventSink(function, event -> {
          });
        }

        @Override
        public Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor processor,
                                                                        Function<Publisher<Event>, Publisher<Event>> processorFunction) {
          return publisher -> from(publisher).handle((event, sink) -> {
            try {
              sink.next(just(event).transform(processor).block());
            } catch (Throwable throwable) {
              sink.error(unwrap(throwable));
            }
          });
        }
      };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return BLOCKING_PROCESSING_STRATEGY_INSTANCE;
  }

}
