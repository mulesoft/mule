/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.WaitStrategy.LITE_BLOCKING;
import static reactor.core.publisher.WorkQueueProcessor.share;
import static reactor.util.concurrent.QueueSupplier.SMALL_BUFFER_SIZE;
import static reactor.util.concurrent.QueueSupplier.isPowerOfTwo;
import static reactor.util.concurrent.WaitStrategy.blocking;
import static reactor.util.concurrent.WaitStrategy.busySpin;
import static reactor.util.concurrent.WaitStrategy.liteBlocking;
import static reactor.util.concurrent.WaitStrategy.parking;
import static reactor.util.concurrent.WaitStrategy.phasedOffLiteLock;
import static reactor.util.concurrent.WaitStrategy.sleeping;
import static reactor.util.concurrent.WaitStrategy.yielding;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.WorkQueueProcessor;

/**
 * Creates ring-buffer based processing strategy instances. These processing strategy de-multiplex incoming messages using a
 * ring-buffer which can then be subscribed to n times.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public abstract class AbstractRingBufferProcessingStrategyFactory implements ProcessingStrategyFactory {

  protected static String RING_BUFFER_SCHEDULER_NAME_SUFFIX = ".ring-buffer";

  public static int DEFAULT_BUFFER_SIZE = SMALL_BUFFER_SIZE;
  public static int DEFAULT_SUBSCRIBER_COUNT = 1;
  public static String DEFAULT_WAIT_STRATEGY = LITE_BLOCKING.name();

  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private int subscriberCount = DEFAULT_SUBSCRIBER_COUNT;
  private String waitStrategy;

  /**
   * Configure the size of the ring-buffer size used to buffer and de-multiplex events from multiple source threads. This value
   * must be a power-of two.
   * <p/>
   * Ring buffers typically use a power of two because it means that the rollover at the end of the buffer can be achieved using a
   * bit mask rather than having to explicitly compare the head/tail pointer with the end of the buffer.
   *
   * @param bufferSize buffer size to use.
   */
  public void setBufferSize(int bufferSize) {
    if (!isPowerOfTwo(bufferSize)) {
      throw new IllegalArgumentException("bufferSize must be a power of 2 : " + bufferSize);
    }
    this.bufferSize = bufferSize;
  }

  /**
   * Configure the number of ring-buffer subscribers.
   *
   * @param subscriberCount
   */
  public void setSubscriberCount(int subscriberCount) {
    this.subscriberCount = subscriberCount;
  }

  /**
   * Configure the wait strategy used to wait for new events on ring-buffer.
   *
   * @param waitStrategy
   */
  public void setWaitStrategy(String waitStrategy) {
    this.waitStrategy = waitStrategy;
  }

  protected int getBufferSize() {
    return bufferSize;
  }

  protected static int getSubscriberCount() {
    return DEFAULT_SUBSCRIBER_COUNT;
  }

  protected String getWaitStrategy() {
    return waitStrategy;
  }

  static protected class RingBufferProcessingStrategy extends AbstractProcessingStrategy {

    private Supplier<Scheduler> ringBufferSchedulerSupplier;
    private int bufferSize;
    private int subscribers;
    private WaitStrategy waitStrategy = WaitStrategy.valueOf(DEFAULT_WAIT_STRATEGY);
    private MuleContext muleContext;

    public RingBufferProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                        String waitStrategy,
                                        MuleContext muleContext) {
      this.ringBufferSchedulerSupplier = ringBufferSchedulerSupplier;
      this.bufferSize = bufferSize;
      this.subscribers = subscribers;
      if (waitStrategy != null) {
        this.waitStrategy = WaitStrategy.valueOf(waitStrategy);
      }
      this.muleContext = muleContext;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
      WorkQueueProcessor<Event> processor =
          share(ringBufferSchedulerSupplier.get(), bufferSize, waitStrategy.getReactorWaitStrategy(), false);
      List<reactor.core.Disposable> disposables = new ArrayList<>();
      for (int i = 0; i < subscribers; i++) {
        disposables.add(processor.transform(function).retry().subscribe());
      }
      return new ReactorSink(processor.connectSink(), flowConstruct,
                             () -> disposables.forEach(disposable -> disposable.dispose()),
                             createOnEventConsumer());
    }

    protected MuleContext getMuleContext() {
      return this.muleContext;
    }

  }

  protected enum WaitStrategy {
    BLOCKING(blocking()),

    LITE_BLOCKING(liteBlocking()),

    SLEEPING(sleeping()),

    BUSY_SPIN(busySpin()),

    YIELDING(yielding()),

    PARKING(parking()),

    PHASED(phasedOffLiteLock(200, 100, TimeUnit.MILLISECONDS));

    private reactor.util.concurrent.WaitStrategy reactorWaitStrategy;

    WaitStrategy(reactor.util.concurrent.WaitStrategy reactorWaitStrategy) {
      this.reactorWaitStrategy = reactorWaitStrategy;
    }

    reactor.util.concurrent.WaitStrategy getReactorWaitStrategy() {
      return reactorWaitStrategy;
    }
  }
}
