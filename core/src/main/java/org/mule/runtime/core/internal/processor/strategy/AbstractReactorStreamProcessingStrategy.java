/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

abstract class AbstractReactorStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

  private final Supplier<Scheduler> cpuLightSchedulerSupplier;
  private Scheduler cpuLightScheduler;
  private final int parallelism;
  private SchedulerService schedulerService;

  AbstractReactorStreamProcessingStrategy(int subscribers,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier, int parallelism,
                                          int maxConcurrency, boolean maxConcurrencyEagerCheck) {
    super(subscribers, maxConcurrency, maxConcurrencyEagerCheck);
    this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    this.parallelism = parallelism;
  }

  AbstractReactorStreamProcessingStrategy(int subscribers,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier, int parallelism,
                                          int maxConcurrency, boolean maxConcurrencyEagerCheck,
                                          SchedulerService schedulerService) {
    super(subscribers, maxConcurrency, maxConcurrencyEagerCheck);
    this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    this.parallelism = parallelism;
    this.schedulerService = schedulerService;
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    reactor.core.scheduler.Scheduler cpuLiteScheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
    if (processor.getProcessingType() == CPU_LITE_ASYNC) {
      return onNonBlockingProcessorTxAware(publisher -> from(publisher)
          .transform(processor)
          .publishOn(cpuLiteScheduler)
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler())));
    } else {
      return publisher -> from(publisher)
          .transform(processor)
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
    }
  }

  @Override
  protected ScheduledExecutorService decorateScheduler(ScheduledExecutorService scheduler) {
    return new ConditionalExecutorServiceDecorator(scheduler,
                                                   scheduledExecutorService -> schedulerService.isCurrentThreadForCpuWork());
  }

  protected ReactiveProcessor onNonBlockingProcessorTxAware(ReactiveProcessor processor) {
    return processor;
  }

  protected int getParallelism() {
    return parallelism;
  }

  @Override
  public void start() throws MuleException {
    this.cpuLightScheduler = createCpuLightScheduler(cpuLightSchedulerSupplier);
  }

  protected Scheduler createCpuLightScheduler(Supplier<Scheduler> cpuLightSchedulerSupplier) {
    return cpuLightSchedulerSupplier.get();
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
