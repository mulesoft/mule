/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Sink;

import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Determines how a list of message processors should processed.
 */
public interface ProcessingStrategy {

  /**
   * Creates instances of {@link Sink} to be used for emitting {@link Event}'s to be processed. Each {@link Sink} should be use
   * independent streams that implement the {@link Pipeline}.
   *
   * @param flowConstruct pipeline instance.
   * @param function function representing the
   * @return new sink instance
   */
  Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function);

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

  /**
   * Whether the processing that has this instance is synchronous or not
   */
  default boolean isSynchronous() {
    return false;
  }

}
