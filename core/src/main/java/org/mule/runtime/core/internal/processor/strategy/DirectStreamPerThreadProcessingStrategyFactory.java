/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static reactor.util.concurrent.Queues.SMALL_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * Processing strategy that processes the {@link Pipeline} in the caller thread and does not schedule the processing of any
 * {@link Processor} in a different thread pool regardless of their {@link ProcessingType}. While processing of the flow is
 * carried out in the caller thread, when a {@link Processor} implements non-blocking behaviour then processing will continue in a
 * {@link Processor} thread.
 * <p/>
 * This implementation offers an optimization over {@link DirectProcessingStrategyFactory} by reusing a single stream instance for
 * each callee thread.
 *
 * @since 4.0
 */
public class DirectStreamPerThreadProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy DIRECT_STREAM_PER_THREAD_PROCESSING_STRATEGY_INSTANCE =
      new AbstractProcessingStrategy() {

        @Override
        public boolean isSynchronous() {
          return true;
        }

        @Override
        public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
          return new PerThreadSink(() -> new DirectSink(pipeline, event -> {
          }, SMALL_BUFFER_SIZE));
        }
      };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return DIRECT_STREAM_PER_THREAD_PROCESSING_STRATEGY_INSTANCE;
  }

}
