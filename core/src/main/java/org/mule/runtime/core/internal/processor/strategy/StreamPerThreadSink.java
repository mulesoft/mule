/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.yield;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link Sink} implementation that uses a {@link Flux} for each thread that dispatches events to it.
 */
public class StreamPerThreadSink implements Sink, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamPerThreadSink.class);
  private static final int THREAD_CACHE_TIME_LIMIT_IN_MINUTES = 60;
  private static final int TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES = 10;

  private final ReactiveProcessor processor;
  private final Consumer<CoreEvent> eventConsumer;
  private final FlowConstruct flowConstruct;

  private volatile boolean disposing = false;
  private final Cache<Thread, FluxSink<CoreEvent>> sinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, FluxSink<CoreEvent>>) (thread, coreEventFluxSink,
                                                                           removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();
  private final Cache<Transaction, FluxSink<CoreEvent>> sinksNestedTx =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Transaction, FluxSink<CoreEvent>>) (transaction, coreEventFluxSink,
                                                                                removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();
  // We add this counter so we can count the amount of finished sinks when disposing
  // The previous way involved having a strong reference to the thread, which caused MULE-19209
  private AtomicLong disposableSinks = new AtomicLong();

  /**
   * Creates a {@link StreamPerThreadSink}.
   *
   * @param processor     the processor to process events emitted onto stream, typically this processor will represent the flow
   *                      pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public StreamPerThreadSink(ReactiveProcessor processor, Consumer<CoreEvent> eventConsumer, FlowConstruct flowConstruct) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
    this.flowConstruct = flowConstruct;
  }

  private FluxSink<CoreEvent> createSink() {
    disposableSinks.incrementAndGet();
    final FluxSinkRecorder<CoreEvent> recorder = new FluxSinkRecorder<>();
    recorder.flux()
        .doOnNext(request -> eventConsumer.accept(request))
        .transform(processor)
        .subscribe(null, e -> {
          LOGGER.error("Exception reached PS subscriber for flow '" + flowConstruct.getName() + "'", e);
          disposableSinks.decrementAndGet();
        }, () -> disposableSinks.decrementAndGet());

    return recorder.getFluxSink();
  }

  @Override
  public void accept(CoreEvent event) {
    if (disposing) {
      throw new IllegalStateException("Already disposed");
    }

    TransactionCoordination txCoord = TransactionCoordination.getInstance();
    if (txCoord.runningNestedTransaction()) {
      sinksNestedTx.get(txCoord.getTransaction(), tx -> createSink()).next(event);
    } else {
      sinks.get(currentThread(), t -> createSink()).next(event);
    }
  }

  @Override
  public BackPressureReason emit(CoreEvent event) {
    accept(event);
    return null;
  }

  @Override
  public void dispose() {
    disposing = true;
    sinks.asMap().values().forEach(sink -> sink.complete());
    sinksNestedTx.asMap().values().forEach(sink -> sink.complete());

    final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
    long startMillis = currentTimeMillis();

    while (disposableSinks.get() != 0
        && currentTimeMillis() <= shutdownTimeout + startMillis
        && !currentThread().isInterrupted()) {
      yield();
    }

    if (currentThread().isInterrupted()) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed before thread interruption",
                                               flowConstruct.getName()));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed before thread interruption",
                    flowConstruct.getName());
      }
      sinks.invalidateAll();
      sinksNestedTx.invalidateAll();
    } else if (disposableSinks.get() != 0) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed in %d ms",
                                               flowConstruct.getName(),
                                               shutdownTimeout));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                    shutdownTimeout);
      }
      sinks.invalidateAll();
      sinksNestedTx.invalidateAll();
    }
  }
}
