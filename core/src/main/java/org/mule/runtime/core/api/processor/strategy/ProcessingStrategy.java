/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.RejectedExecutionException;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * Determines how a list of message processors should processed.
 */
@NoImplement
public interface ProcessingStrategy {

  /**
   * Creates instances of {@link Sink} to be used for emitting {@link CoreEvent}'s to be processed. Each {@link Sink} should be
   * used independent streams that implement the {@link Pipeline}.
   *
   * @param flowConstruct pipeline instance.
   * @param pipeline function representing the pipeline.
   * @return new sink instance
   */
  Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline);

  /**
   * For sinks created internally by the components in a flow, have them accounted for in the processing strategy for a graceful
   * shutdown.
   *
   * @param flux the flux whose sink will be registered
   * @param sinkRepresentation a representation of the chain for it to appear in log entries.
   */
  default void registerInternalSink(Publisher<CoreEvent> flux, String sinkRepresentation) {
    Flux.from(flux).subscribe();
  }

  default Publisher<CoreEvent> registerInternalFlux(Publisher<CoreEvent> flux) {
    return flux;
  }

  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param pipeline processor representing the the pipeline.
   * @return enriched pipeline function
   */
  default ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    return pipeline;
  }

  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param processor processor instance.
   * @return enriched processor function
   */
  default ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return processor;
  }

  /**
   * Whether the processing that has this instance is synchronous or not
   */
  default boolean isSynchronous() {
    return false;
  }

  /**
   * Checks whether backpressure will be fired for a new accepted {@link org.mule.runtime.api.event.Event} to be processed. The
   * event is attempted to be accepted for processing into the flow. If it succeeds, processing begins with the corresponding
   * {@link ProcessingStrategy}. If not, a backpressure signal is raised, and a {@link RejectedExecutionException} is thrown.
   *
   * @throws RejectedExecutionException
   */
  default void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {}

  /**
   * Checks whether backpressure will be fired for a new accepted {@link org.mule.runtime.api.event.Event} to be processed. The
   * event is attempted to be accepted for processing into the flow. If it succeeds, processing begins with the corresponding
   * {@link ProcessingStrategy}. If not, a backpressure signal is raised, and the function returns false.
   *
   * @return null if the event is accepted by the {@link ProcessingStrategy}. Otherwise, the reason why it was not accepted.
   */
  default BackPressureReason checkBackpressureEmitting(CoreEvent event) {
    return null;
  }
}
