/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * Creates {@link ProactorProcessingStrategyFactory} instances. This processing strategy dipatches incoming messages to
 * single-threaded event-loop. The execution
 *
 *
 * Processing of the flow is carried out on the event-loop but which is served by a pool of worker threads from the applications
 * IO {@link Scheduler}. Processing of the flow is carried out synchronously on the worker thread until completion.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ProactorProcessingStrategyFactory extends AbstractRingBufferProcessingStrategyFactory {

  private static int DEFAULT_MAX_CONCURRENCY = Integer.MAX_VALUE;
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

  protected int getMaxConcurrency() {
    return maxConcurrency;
  }

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    if (maxConcurrency == 1) {
      return new ReactorProcessingStrategyFactory().create(muleContext, schedulersNamePrefix);
    } else {
      return new ProactorProcessingStrategy(() -> muleContext.getSchedulerService()
          .cpuLightScheduler(config().withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                            () -> muleContext.getSchedulerService()
                                                .ioScheduler(config().withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                            () -> muleContext.getSchedulerService()
                                                .cpuIntensiveScheduler(config()
                                                    .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                            scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                        MILLISECONDS),
                                            maxConcurrency,
                                            () -> muleContext.getSchedulerService()
                                                .customScheduler(config()
                                                    .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
                                                    .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                            getBufferSize(),
                                            getSubscriberCount(),
                                            getWaitStrategy(),
                                            muleContext);
    }
  }

  static class ProactorProcessingStrategy extends RingBufferProcessingStrategy implements Startable, Stoppable {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Supplier<Scheduler> blockingSchedulerSupplier;
    private Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;
    private Scheduler cpuLightScheduler;
    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;
    private int maxConcurrency;

    public ProactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                      Supplier<Scheduler> blockingSchedulerSupplier,
                                      Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                      Consumer<Scheduler> schedulerStopper,
                                      int maxConcurrency,
                                      Supplier<Scheduler> ringBufferSchedulerSupplier,
                                      int bufferSize,
                                      int subscriberCount,
                                      String waitStrategy,
                                      MuleContext muleContext) {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, muleContext);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
      this.blockingSchedulerSupplier = blockingSchedulerSupplier;
      this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
      this.schedulerStopper = schedulerStopper;
      this.maxConcurrency = maxConcurrency;
    }

    @Override
    public void start() throws MuleException {
      this.cpuLightScheduler = cpuLightSchedulerSupplier.get();
      this.blockingScheduler = blockingSchedulerSupplier.get();
      this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (cpuLightScheduler != null) {
        schedulerStopper.accept(cpuLightScheduler);
      }
      if (blockingScheduler != null) {
        schedulerStopper.accept(blockingScheduler);
      }
      if (cpuIntensiveScheduler != null) {
        schedulerStopper.accept(cpuIntensiveScheduler);
      }
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction) {
      // TODO MULE-11775 Potential race condition in ProactorProcessingStrategy.
      //return publisher -> from(publisher).parallel(min(getRuntime().availableProcessors(), maxConcurrency))
      //    .runOn(fromExecutorService(getExecutorService(cpuLightScheduler))).composeGroup(pipelineFunction);
      return super.onPipeline(flowConstruct, pipelineFunction);
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor messageProcessor,
                                                                    Function<Publisher<Event>, Publisher<Event>> processorFunction) {
      if (messageProcessor.getProcessingType() == BLOCKING) {
        return proactor(processorFunction, blockingScheduler);
      } else if (messageProcessor.getProcessingType() == CPU_INTENSIVE) {
        return proactor(processorFunction, cpuIntensiveScheduler);
      } else {
        return publisher -> from(publisher).transform(processorFunction);
      }
    }

    private Function<Publisher<Event>, Publisher<Event>> proactor(Function<Publisher<Event>, Publisher<Event>> processorFunction,
                                                                  Scheduler scheduler) {
      return publisher -> from(publisher).flatMap(event -> just(event).transform(processorFunction)
          .publishOn(fromExecutorService(getExecutorService(cpuLightScheduler)))
          .subscribeOn(fromExecutorService(getExecutorService(scheduler))), maxConcurrency);
    }

    protected ExecutorService getExecutorService(Scheduler scheduler) {
      return new ConditionalExecutorServiceDecorator(scheduler, scheduleOverridePredicate());
    }

    /**
     * Provides a way override the scheduling of tasks based on a predicate.
     *
     * @return preficate that determines if task should be scheduled or processed in the current thread.
     */
    protected Predicate<Scheduler> scheduleOverridePredicate() {
      return scheduler -> false;
    }

  }

}
