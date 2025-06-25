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

import java.util.ArrayList;
import java.util.List;
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

  private boolean sinkIndexEnabled;

  private final Cache<Thread, List<FluxSinkWrapper>> sinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, List<FluxSinkWrapper>>) (String, coreEventFluxSinkList,
                                                                             removalCause) -> coreEventFluxSinkList
                                                                                 .forEach(FluxSinkWrapper::complete))
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  private final Cache<Thread, FluxSink<CoreEvent>> legacySinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, FluxSink<CoreEvent>>) (thread, coreEventFluxSink,
                                                                           removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  private final Cache<Transaction, List<FluxSinkWrapper>> sinksNestedTx =
      Caffeine.newBuilder()
          .removalListener((RemovalListener<Transaction, List<FluxSinkWrapper>>) (transaction, coreEventFluxSinkList,
                                                                                  removalCause) ->

          coreEventFluxSinkList.forEach(FluxSinkWrapper::complete))
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  private final Cache<Transaction, FluxSink<CoreEvent>> legacySinksNestedTx =
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

  public StreamPerThreadSink(ReactiveProcessor processor, Consumer<CoreEvent> eventConsumer, FlowConstruct flowConstruct,
                             boolean sinkIndexEnabled) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
    this.flowConstruct = flowConstruct;
    this.sinkIndexEnabled = sinkIndexEnabled;
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
      if (sinkIndexEnabled) {
        getNestedTxFluxSinkWrapper(txCoord).next(event);
      } else {
        legacySinksNestedTx.get(txCoord.getTransaction(), tx -> createSink()).next(event);
      }
    } else {
      if (sinkIndexEnabled) {
        getSimpleFluxSinkWrapper().next(event);
      } else {
        legacySinks.get(currentThread(), t -> createSink()).next(event);
      }
    }
  }

  private FluxSink<CoreEvent> getNestedTxFluxSinkWrapper(TransactionCoordination txCoord) {
    return getOrCreateFluxSinkWrapper(sinksNestedTx.get(txCoord.getTransaction(), parameterKey -> new ArrayList<>()));
  }


  private FluxSinkWrapper getSimpleFluxSinkWrapper() {
    return getOrCreateFluxSinkWrapper(sinks.get(currentThread(), parameterKey -> new ArrayList<>()));
  }

  private FluxSinkWrapper getOrCreateFluxSinkWrapper(List<FluxSinkWrapper> fluxSinkWrapperList) {
    for (FluxSinkWrapper fluxSinkWrapper : fluxSinkWrapperList) {
      if (fluxSinkWrapper.isBeingUsed()) {
        continue;
      }

      return fluxSinkWrapper;
    }

    FluxSinkWrapper fluxSinkWrapper = new FluxSinkWrapper(createSink());
    fluxSinkWrapperList.add(fluxSinkWrapper);
    return fluxSinkWrapper;
  }

  @Override
  public BackPressureReason emit(CoreEvent event) {
    accept(event);
    return null;
  }

  @Override
  public void dispose() {
    disposing = true;
    sinks.asMap().values().forEach(sinkList -> sinkList.forEach(FluxSinkWrapper::complete));
    legacySinks.asMap().values().forEach(FluxSink::complete);
    sinksNestedTx.asMap().values().forEach(sinkList -> sinkList.forEach(FluxSinkWrapper::complete));
    legacySinksNestedTx.asMap().values().forEach(FluxSink::complete);

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
      LOGGER.warn("invalidate all shinks: {}",sinks);
      sinks.invalidateAll();
      LOGGER.warn("invalidate all sinksNestedTx: {}",sinksNestedTx);
      sinksNestedTx.invalidateAll();
      legacySinks.invalidateAll();
      legacySinksNestedTx.invalidateAll();
    } else if (disposableSinks.get() != 0) {
      long remainingDisposableSinks = disposableSinks.get();
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers %d of ProcessingStrategy for flow '%s' not completed in %d ms",
                                               remainingDisposableSinks,
                                               flowConstruct.getName(),
                                               shutdownTimeout));
      } else {
        LOGGER.warn("TX Subscribers {} of ProcessingStrategy for flow '{}' not completed in {} ms", remainingDisposableSinks, flowConstruct.getName(),
                    shutdownTimeout);
      }
      LOGGER.warn("invalidate all shinks: {}",sinks);
      sinks.invalidateAll();
      LOGGER.warn("invalidate all sinksNestedTx: {}",sinksNestedTx);
      sinksNestedTx.invalidateAll();
      legacySinks.invalidateAll();
      legacySinksNestedTx.invalidateAll();
    }
  }
}
