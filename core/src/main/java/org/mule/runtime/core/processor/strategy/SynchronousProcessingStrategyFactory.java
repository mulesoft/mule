/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import com.google.common.cache.Cache;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.DirectProcessor;

/**
 * Processing strategy that processes all {@link Processor}'s in the caller thread. Unlike other, asynchronous, processing
 * strategies this processing strategy does not used a shared stream, given this would require serializing all requests and
 * limiting the effectiveness of multi-threaded sources and operations. Use {@link SynchronousStreamProcessingStrategyFactory} in
 * order to obtain stream semantics while doing all processing in the caller thread.
 */
public class SynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE =
      new AbstractProcessingStrategy() {

        @Override
        public boolean isSynchronous() {
          return true;
        }

        @Override
        public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
          return new PerThreadSink(() -> new DirectSink(function, event -> {
          }));
        }
      };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }

  static class PerThreadSink implements Sink, Disposable {

    private Supplier<Sink> sinkSupplier;
    private Cache<Thread, Sink> sinkCache =
        newBuilder().weakValues().removalListener(notification -> disposeIfNeeded(notification.getValue(), NOP_LOGGER)).build();

    /**
     * Create a {@link PerThreadSink} that will create and use a given
     * {@link Sink} for each distinct caller {@link Thread}.
     *
     * @param sinkSupplier {@link Supplier} for the {@link Sink} that sould be used for each thread.
     */
    public PerThreadSink(Supplier<Sink> sinkSupplier) {
      this.sinkSupplier = sinkSupplier;
    }

    @Override
    public void accept(Event event) {
      try {
        sinkCache.get(currentThread(), () -> sinkSupplier.get()).accept(event);
      } catch (ExecutionException e) {
        throw new IllegalStateException("Unable to create Sink for Thread " + currentThread(), e);
      }
    }

    @Override
    public void dispose() {
      disposeIfNeeded(sinkCache.asMap().entrySet(), NOP_LOGGER);
      sinkCache.invalidateAll();
    }

  }

  /**
   * {@link Sink} implementation that emits {@link Event}'s via a single stream directly without any de-multiplexing or buffering.
   * If this {@link Sink} is called from multiplex source or client threads then {@link Event}'s will be serialized.
   */
  static class DirectSink implements Sink, Disposable {

    private AbstractProcessingStrategy.ReactorSink reactorSink;

    /**
     * Create new {@link DirectSink}.
     *
     * @param function the processor to process events emitted onto stream, typically this processor will represent the flow
     *        pipeline.
     * @param eventConsumer event consumer called just before {@link Event}'s emission.
     */
    public DirectSink(Function<Publisher<Event>, Publisher<Event>> function, Consumer<Event> eventConsumer) {
      DirectProcessor<Event> directProcessor = DirectProcessor.create();
      BlockingSink<Event> blockingSink = directProcessor.serialize().connectSink();
      reactorSink =
          new AbstractProcessingStrategy.ReactorSink(blockingSink, directProcessor.transform(function).retry().subscribe(),
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

}
