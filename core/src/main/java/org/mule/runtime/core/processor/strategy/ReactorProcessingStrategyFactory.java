/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Creates {@link RingBufferProcessingStrategy} instance that implements the reactor pattern by de-multiplexes incoming messages
 * onto a single event-loop using a ring-buffer.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorProcessingStrategyFactory extends AbstractRingBufferProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorProcessingStrategy(() -> muleContext.getSchedulerService().cpuLightScheduler(),
                                         scheduler -> scheduler.stop(),
                                         () -> muleContext.getSchedulerService()
                                             .customScheduler(muleContext.getSchedulerBaseConfig()
                                                 .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
                                                 .withMaxConcurrentTasks(getSubscriberCount() + 1)),

                                         getBufferSize(),
                                         getSubscriberCount(),
                                         getWaitStrategy(),
                                         muleContext);
  }

  static class ReactorProcessingStrategy extends RingBufferProcessingStrategy implements Startable, Stoppable {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;
    private Scheduler cpuLightScheduler;

    public ReactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                     Consumer<Scheduler> schedulerStopper,
                                     Supplier<Scheduler> ringBufferSchedulerSupplier,
                                     int bufferSize,
                                     int subscriberCount,
                                     String waitStrategy,
                                     MuleContext muleContext) {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, muleContext);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
      this.schedulerStopper = schedulerStopper;
    }

    @Override
    public void start() throws MuleException {
      this.cpuLightScheduler = cpuLightSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (cpuLightScheduler != null) {
        schedulerStopper.accept(cpuLightScheduler);
      }
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return publisher -> from(publisher).publishOn(fromExecutorService(getExecutorService(cpuLightScheduler)))
          .transform(pipeline);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor)
            .publishOn(fromExecutorService(getExecutorService(cpuLightScheduler)));
      } else {
        return super.onProcessor(processor);
      }
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

    protected Scheduler getCpuLightScheduler() {
      return cpuLightScheduler;
    }
  }

}
