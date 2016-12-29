/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.empty;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.rx.Exceptions.EventDroppedException;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.Cancellation;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;

/**
 * Determines how a list of message processors should processed.
 */
public interface ProcessingStrategy {

  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param flowConstruct pipeline instance.
   * @param pipelineFunction pipeline function.
   * @return enriched pipeline function/
   */
  default Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                  Function<Publisher<Event>, Publisher<Event>> pipelineFunction) {
    return onPipeline(flowConstruct, pipelineFunction, flowConstruct.getExceptionListener());
  }

  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param flowConstruct pipeline instance.
   * @param pipelineFunction pipeline function.
   * @param messagingExceptionHandler exception handle to use.
   * @return enriched pipeline function
   */
  default Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                  Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                  MessagingExceptionHandler messagingExceptionHandler) {
    return publisher -> from(publisher).transform(pipelineFunction);
  }


  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param processor processor instance.
   * @param processorFunction processor function
   * @return enriched processor function
   */
  default Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor processor,
                                                                   Function<Publisher<Event>, Publisher<Event>> processorFunction) {
    return publisher -> from(publisher).transform(processorFunction);
  }

  default Disposable subscribe(FlowConstruct flowConstruct, Publisher<Event> publisher) {
    Cancellation cancellation = from(publisher)
        .onErrorResumeWith(MessagingException.class, flowConstruct.getExceptionListener())
        .doOnNext(response -> response.getContext().success(response))
        // We no longer need error here, so supress error to avoid retry.
        .doOnError(MessagingException.class, me -> me.getEvent().getContext().error(me))
        .onErrorResumeWith(EventDroppedException.class, ede -> {
          ede.getEvent().getContext().success();
          return empty();
        })
        .retry()
        .subscribe();
    return () -> cancellation.dispose();
  }

  default Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> processorFunction) {
    FluxProcessor<Event, Event> processor = EmitterProcessor.create(false);
    Flux flux = processor.transform(processorFunction);
    Disposable cancellation = subscribe(flowConstruct, flux);
    BlockingSink<Event> sink = processor.connectSink();
    return new Sink() {

      @Override
      public void accept(Event event) {
        sink.accept(event);
      }

      @Override
      public void complete() {
        sink.complete();
        sink.cancel();
        cancellation.dispose();
      }
    };
  }

  /**
   * Whether the processing that has this instance is synchronous or not
   */
  default boolean isSynchronous() {
    return false;
  }

}
