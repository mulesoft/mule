/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategy.WaitStrategy.LITE_BLOCKING;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.util.concurrent.QueueSupplier.SMALL_BUFFER_SIZE;
import static reactor.util.concurrent.QueueSupplier.isPowerOfTwo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.function.Supplier;

/**
 * Creates {@link WorkQueueStreamProcessingStrategy} instances that de-multiplexes incoming messages using a ring-buffer but
 * instead of processing events using a constrained {@link SchedulerService#cpuLightScheduler()}, or by using the proactor
 * pattern, instead simply performs all processing on a larger work queue pool using a fixed number of threads from the
 * {@link SchedulerService#ioScheduler()}.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueStreamProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

  protected static String RING_BUFFER_SCHEDULER_NAME_SUFFIX = ".ring-buffer";

  public static int DEFAULT_BUFFER_SIZE = SMALL_BUFFER_SIZE;
  public static int DEFAULT_SUBSCRIBER_COUNT = 1;
  public static String DEFAULT_WAIT_STRATEGY = LITE_BLOCKING.name();

  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private int subscriberCount = DEFAULT_SUBSCRIBER_COUNT;
  private String waitStrategy;

  /**
   * Configure the size of the ring-buffer size used to buffer and de-multiplexes events from multiple source threads. This value
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

  protected int getSubscriberCount() {
    return subscriberCount;
  }

  protected String getWaitStrategy() {
    return waitStrategy;
  }


  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueStreamProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                                 getBufferSize(),
                                                 getSubscriberCount(),
                                                 getWaitStrategy(),
                                                 () -> muleContext.getSchedulerService()
                                                     .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                         .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                 getMaxConcurrency());
  }

  static protected class WorkQueueStreamProcessingStrategy extends AbstractRingBufferProcessingStrategy
      implements Startable, Stoppable {

    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private Scheduler blockingScheduler;

    protected WorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                int subscribers,
                                                String waitStrategy, Supplier<Scheduler> blockingSchedulerSupplier,
                                                int maxConcurrency) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, maxConcurrency);
      this.blockingSchedulerSupplier = requireNonNull(blockingSchedulerSupplier);
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency > subscribers) {
        return publisher -> from(publisher)
            .flatMap(event -> just(event).transform(pipeline).subscribeOn(fromExecutorService(blockingScheduler)),
                     maxConcurrency);
      } else {
        return super.onPipeline(pipeline);
      }
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher)
            .flatMap(event -> just(event).transform(processor).subscribeOn(fromExecutorService(blockingScheduler)),
                     maxConcurrency);
      } else {
        return super.onProcessor(processor);
      }
    }

    @Override
    public void start() throws MuleException {
      this.blockingScheduler = blockingSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (blockingScheduler != null) {
        blockingScheduler.stop();
      }
    }

  }


}
