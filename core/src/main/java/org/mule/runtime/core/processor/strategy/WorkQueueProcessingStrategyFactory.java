/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates {@link WorkQueueProcessingStrategy} instances. This processing strategy dispatches incoming messages to a work queue
 * which is served by a pool of worker threads from the applications IO {@link Scheduler}. While processing of the flow is carried
 * out using a single worker thread when a {@link Processor} implements non-blocking behaviour (e.g. an outbound HTTP request)
 * then processing will continue in a {@link Processor} thread.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueProcessingStrategyFactory extends AbstractRingBufferProcessingStrategyFactory {

  private static int DEFAULT_MAX_CONCURRENCY = 16;
  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

  /**
   * Configures the maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks
   * using the IO pool, but will also limit the number of CPU_LIGHT threads in use concurrently.
   *
   * @param maxConcurrency
   */
  public void setMaxConcurrency(int maxConcurrency) {
    if (maxConcurrency < 1) {
      throw new IllegalArgumentException("maxConcurrency must be at least 1");
    }
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(muleContext.getSchedulerBaseConfig().withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                           maxConcurrency,
                                           scheduler -> scheduler.stop(),
                                           () -> muleContext.getSchedulerService()
                                               .customScheduler(muleContext.getSchedulerBaseConfig()
                                                   .withName(schedulersNamePrefix + "." + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
                                                   .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                           getBufferSize(),
                                           getSubscriberCount(),
                                           getWaitStrategy(),
                                           muleContext);
  }

  /**
   * Configure the maximum concurrency for message processing in the flow.
   *
   * @param concurrency
   */
  public void setConcurrency(int concurrency) {
    this.maxConcurrency = concurrency;
  }

  static class WorkQueueProcessingStrategy extends RingBufferProcessingStrategy implements Startable, Stoppable {

    private Supplier<Scheduler> ioSchedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;
    private int concurrency;
    private Scheduler ioScheduler;

    public WorkQueueProcessingStrategy(Supplier<Scheduler> ioSchedulerSupplier, int concurrency,
                                       Consumer<Scheduler> schedulerStopper,
                                       Supplier<Scheduler> ringBufferSchedulerSupplier,
                                       int bufferSize, int subscriberCount, String waitStrategy, MuleContext muleContext) {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, muleContext);
      this.ioSchedulerSupplier = ioSchedulerSupplier;
      this.schedulerStopper = schedulerStopper;
      this.concurrency = concurrency;
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return publisher -> from(publisher)
          .flatMap(event -> just(event).transform(pipeline).subscribeOn(fromExecutorService(ioScheduler)), concurrency);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor).publishOn(fromExecutorService(ioScheduler));
      } else {
        return super.onProcessor(processor);
      }
    }

    @Override
    public void start() throws MuleException {
      this.ioScheduler = ioSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (ioScheduler != null) {
        schedulerStopper.accept(ioScheduler);
      }
    }

  }


}
