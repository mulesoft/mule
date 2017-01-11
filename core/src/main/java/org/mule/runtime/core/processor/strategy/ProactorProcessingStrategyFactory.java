/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.function.Consumer;
import java.util.function.Function;
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
public class ProactorProcessingStrategyFactory extends MultiReactorProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorProcessingStrategy(() -> muleContext.getSchedulerService()
        .cpuLightScheduler(config().withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                          () -> muleContext.getSchedulerService()
                                              .ioScheduler(config().withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                          () -> muleContext.getSchedulerService()
                                              .cpuIntensiveScheduler(config()
                                                  .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                          scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                      MILLISECONDS),
                                          muleContext);
  }

  static class ProactorProcessingStrategy extends ReactorProcessingStrategy {

    private Supplier<Scheduler> blockingSchedulerSupplier;
    private Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;

    public ProactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                      Supplier<Scheduler> blockingSchedulerSupplier,
                                      Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                      Consumer<Scheduler> schedulerStopper,
                                      MuleContext muleContext) {
      super(cpuLightSchedulerSupplier, schedulerStopper, muleContext);
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
        getSchedulerStopper().accept(blockingScheduler);
      }
      if (cpuIntensiveScheduler != null) {
        getSchedulerStopper().accept(cpuIntensiveScheduler);
      }
      super.stop();
    }

    //@Override
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
      return publisher -> from(publisher)
          .publishOn(fromExecutorService(getExecutorService(scheduler)))
          .transform(processorFunction)
          .publishOn(fromExecutorService(getExecutorService(cpuLightScheduler)));
    }

  }

}
