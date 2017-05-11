/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategy.WaitStrategy.valueOf;
import static reactor.util.concurrent.WaitStrategy.blocking;
import static reactor.util.concurrent.WaitStrategy.busySpin;
import static reactor.util.concurrent.WaitStrategy.liteBlocking;
import static reactor.util.concurrent.WaitStrategy.parking;
import static reactor.util.concurrent.WaitStrategy.phasedOffLiteLock;
import static reactor.util.concurrent.WaitStrategy.sleeping;
import static reactor.util.concurrent.WaitStrategy.yielding;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reactor.core.Disposable;
import reactor.core.publisher.WorkQueueProcessor;

/**
 * Abstract {@link ProcessingStrategy} to be used by implementations that de-multiplex incoming messages using a ring-buffer which
 * can then be subscribed to n times.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public abstract class AbstractRingBufferProcessingStrategy extends AbstractProcessingStrategy implements Startable, Stoppable {

  final protected Supplier<Scheduler> ringBufferSchedulerSupplier;
  final protected int bufferSize;
  final protected int subscribers;
  final protected WaitStrategy waitStrategy;
  final protected int maxConcurrency;

  public AbstractRingBufferProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                              String waitStrategy,
                                              int maxConcurrency) {
    this.subscribers = requireNonNull(subscribers);
    this.waitStrategy = valueOf(waitStrategy);
    this.bufferSize = requireNonNull(bufferSize);
    this.ringBufferSchedulerSupplier = requireNonNull(ringBufferSchedulerSupplier);
    this.maxConcurrency = requireNonNull(maxConcurrency);
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
    WorkQueueProcessor<Event> processor =
        WorkQueueProcessor.share(ringBufferSchedulerSupplier.get(), bufferSize, waitStrategy.getReactorWaitStrategy(), false);
    List<Disposable> disposables = new ArrayList<>();
    for (int i = 0; i < (maxConcurrency < subscribers ? maxConcurrency : subscribers); i++) {
      disposables.add(processor.transform(function).subscribe());
    }
    return new ReactorSink(processor.connectSink(), () -> disposables.forEach(disposable -> disposable.dispose()),
                           createOnEventConsumer());
  }

  protected enum WaitStrategy {
    BLOCKING(blocking()),

    LITE_BLOCKING(liteBlocking()),

    SLEEPING(sleeping()),

    BUSY_SPIN(busySpin()),

    YIELDING(yielding()),

    PARKING(parking()),

    PHASED(phasedOffLiteLock(200, 100, MILLISECONDS));

    private reactor.util.concurrent.WaitStrategy reactorWaitStrategy;

    WaitStrategy(reactor.util.concurrent.WaitStrategy reactorWaitStrategy) {
      this.reactorWaitStrategy = reactorWaitStrategy;
    }

    reactor.util.concurrent.WaitStrategy getReactorWaitStrategy() {
      return reactorWaitStrategy;
    }
  }

}
