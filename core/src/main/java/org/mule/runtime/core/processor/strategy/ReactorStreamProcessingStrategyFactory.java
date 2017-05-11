/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategy.WaitStrategy.LITE_BLOCKING;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.util.concurrent.QueueSupplier.SMALL_BUFFER_SIZE;
import static reactor.util.concurrent.QueueSupplier.isPowerOfTwo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.function.Supplier;


/**
 * Creates {@link ReactorStreamProcessingStrategyFactory} instances that implements the reactor pattern by de-multiplexes incoming
 * messages onto a single event-loop using a ring-buffer and then using using the {@link SchedulerService#cpuLightScheduler()} to
 * process events from the ring-buffer.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorStreamProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

  protected static String RING_BUFFER_SCHEDULER_NAME_SUFFIX = ".ring-buffer";

  public static final int DEFAULT_BUFFER_SIZE = SMALL_BUFFER_SIZE;
  public static final int DEFAULT_SUBSCRIBER_COUNT = 1;
  public static final String DEFAULT_WAIT_STRATEGY = LITE_BLOCKING.name();

  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private int subscriberCount = DEFAULT_SUBSCRIBER_COUNT;
  private String waitStrategy = DEFAULT_WAIT_STRATEGY;

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
    return new ReactorStreamProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1)), getBufferSize(), getSubscriberCount(), getWaitStrategy(),
                                               () -> muleContext.getSchedulerService()
                                                   .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
                                                       .withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                               getMaxConcurrency());
  }

  static protected class ReactorStreamProcessingStrategy extends AbstractRingBufferProcessingStrategy {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Scheduler cpuLightScheduler;

    protected ReactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                              int subscribers,
                                              String waitStrategy, Supplier<Scheduler> cpuLightSchedulerSupplier,
                                              int maxConcurrency) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, maxConcurrency);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency > subscribers) {
        return publisher -> from(publisher).parallel(getNumCpuLightThreads())
            .runOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())))
            .composeGroup(pipeline);
      } else {
        return super.onPipeline(pipeline);
      }
    }

    private int getNumCpuLightThreads() {
      return min(getRuntime().availableProcessors() * 2, maxConcurrency);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor).parallel(getNumCpuLightThreads())
            .runOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())));
      } else {
        return super.onProcessor(processor);
      }
    }

    @Override
    public void start() throws MuleException {
      this.cpuLightScheduler = cpuLightSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (cpuLightScheduler != null) {
        cpuLightScheduler.stop();
      }
    }

    protected Scheduler getCpuLightScheduler() {
      return cpuLightScheduler;
    }
  }

}
