/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;

/**
 * Determines how a list of message processors should processed.
 */
public interface ProcessingStrategy {

  /**
   * Creates instances of {@link Sink} to be used for emitting {@link CoreEvent}'s to be processed. Each {@link Sink} should be used
   * independent streams that implement the {@link Pipeline}.
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
    return publisher -> from(publisher).transform(pipeline);
  }

  /**
   * Enrich {@link Processor} function by adding pre/post operators to implement processing strategy behaviour.
   *
   * @param processor processor instance.
   * @return enriched processor function
   */
  default ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return publisher -> from(publisher).transform(processor);
  }

  /**
   * Whether the processing that has this instance is synchronous or not
   */
  default boolean isSynchronous() {
    return false;
  }

}
