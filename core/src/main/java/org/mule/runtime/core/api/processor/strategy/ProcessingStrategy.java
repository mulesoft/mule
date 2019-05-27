/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.RejectedExecutionException;

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
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param pipeline processor representing the the pipeline.
   * @return enriched pipeline function/
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
   * Checks whether backpressure will be fired for a new accepted {@link org.mule.runtime.api.event.Event} to be processed.
   *
   * @throws RejectedExecutionException
   */
  default void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {}

  /**
   * Checks whether backpressure will be fired for a new emitted {@link org.mule.runtime.api.event.Event} to be processed.
   *
   * @throws RejectedExecutionException
   */
  default boolean checkBackpressureEmitting(CoreEvent event) {
    return true;
  }
}
