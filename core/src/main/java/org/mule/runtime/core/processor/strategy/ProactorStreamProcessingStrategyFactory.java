/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.function.Supplier;

/**
 * Creates {@link ReactorProcessingStrategyFactory.ReactorProcessingStrategy} instance that implements the proactor pattern by
 * de-multiplexing incoming events onto a single event-loop using a ring-buffer and then using using the
 * {@link SchedulerService#cpuLightScheduler()} to process these events from the ring-buffer. In contrast to the
 * {@link ReactorStreamProcessingStrategy} the proactor pattern treats {@link ProcessingType#CPU_INTENSIVE} and
 * {@link ProcessingType#BLOCKING} processors differently and schedules there execution on dedicated
 * {@link SchedulerService#cpuIntensiveScheduler()} and {@link SchedulerService#ioScheduler()} ()} schedulers.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ProactorStreamProcessingStrategyFactory extends ReactorStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    if (getMaxConcurrency() == 1) {
      return new ReactorProcessingStrategyFactory().create(muleContext, schedulersNamePrefix);
    } else {
      return new ProactorStreamProcessingStrategy(() -> muleContext.getSchedulerService()
          .customScheduler(muleContext.getSchedulerBaseConfig()
              .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
              .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                                  getBufferSize(),
                                                  getSubscriberCount(),
                                                  getWaitStrategy(), () -> muleContext.getSchedulerService()
                                                      .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
                                                          .withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                                  () -> muleContext.getSchedulerService()
                                                      .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                          .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                  () -> muleContext.getSchedulerService()
                                                      .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                          .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                                  getMaxConcurrency());
    }
  }

  static class ProactorStreamProcessingStrategy extends ReactorStreamProcessingStrategy {

    private Supplier<Scheduler> blockingSchedulerSupplier;
    private Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;

    public ProactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                            int bufferSize,
                                            int subscriberCount,
                                            String waitStrategy,
                                            Supplier<Scheduler> cpuLightSchedulerSupplier,
                                            Supplier<Scheduler> blockingSchedulerSupplier,
                                            Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                            int maxConcurrency)

    {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier, maxConcurrency);
      this.blockingSchedulerSupplier = blockingSchedulerSupplier;
      this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
    }

    @Override
    public void start() throws MuleException {
      super.start();
      this.blockingScheduler = blockingSchedulerSupplier.get();
      this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (blockingScheduler != null) {
        blockingScheduler.stop();
      }
      if (cpuIntensiveScheduler != null) {
        cpuIntensiveScheduler.stop();
      }
      super.stop();
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == BLOCKING && maxConcurrency > subscribers) {
        return proactor(processor, blockingScheduler);
      } else if (processor.getProcessingType() == CPU_INTENSIVE && maxConcurrency > subscribers) {
        return proactor(processor, cpuIntensiveScheduler);
      } else {
        return super.onProcessor(processor);
      }
    }

    private ReactiveProcessor proactor(ReactiveProcessor processor, Scheduler scheduler) {
      // MULE-12749 Implement async retry and back-pressure for IO scheduling in ProactorStreamProcessingStrategy
      return publisher -> from(publisher)
          .flatMap(event -> just(event).transform(processor)
              .publishOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())))
              .subscribeOn(fromExecutorService(decorateScheduler(scheduler))), maxConcurrency);
    }

  }

}
