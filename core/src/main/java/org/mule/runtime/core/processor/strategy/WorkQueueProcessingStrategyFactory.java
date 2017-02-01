/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.scheduler.Schedulers;

/**
 * Creates {@link WorkQueueProcessingStrategy} instances. This processing strategy dispatches incoming messages to a work queue
 * which is served by a pool of worker threads from the applications IO {@link Scheduler}. Processing of the flow is carried out
 * synchronously on the worker thread until completion.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueProcessingStrategyFactory extends AbstractRingBufferProcessingStrategyFactory {

  private static int DEFAULT_MAX_CONCURRENCY = 16;
  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

  /**
   * Configures the maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks
   * using the IO pool, but will also limit the number of CPU_LIGHT threads in used concurrently.
   *
   * @param maxConcurrency
   */
  public void setMaxConcurrency(int maxConcurrency) {
    if (maxConcurrency > 1) {
      throw new IllegalArgumentException("maxConcurrency must be at least 1");
    }
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(config().withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                           maxConcurrency,
                                           scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                       MILLISECONDS),
                                           () -> muleContext.getSchedulerService().customScheduler(config()
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
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          .flatMap(event -> just(event).transform(pipelineFunction).subscribeOn(Schedulers.fromExecutorService(ioScheduler)),
                   concurrency);
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
