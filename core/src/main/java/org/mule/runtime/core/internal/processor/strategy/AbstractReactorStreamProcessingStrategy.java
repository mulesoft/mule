/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

abstract class AbstractReactorStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

  private final Supplier<Scheduler> cpuLightSchedulerSupplier;
  private final int parallelism;
  private final AtomicInteger inFlightEvents = new AtomicInteger();
  private final BiConsumer<CoreEvent, Throwable> inFlightDecrementCallback = (e, t) -> inFlightEvents.decrementAndGet();

  private Scheduler cpuLightScheduler;

  AbstractReactorStreamProcessingStrategy(int subscribers,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier,
                                          int parallelism,
                                          int maxConcurrency,
                                          boolean maxConcurrencyEagerCheck) {
    super(subscribers, maxConcurrency, maxConcurrencyEagerCheck);
    this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    this.parallelism = parallelism;
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    if (processor.getProcessingType() == CPU_LITE_ASYNC) {
      reactor.core.scheduler.Scheduler cpuLiteScheduler = fromExecutorService(getNonBlockingTaskScheduler());
      return publisher -> from(publisher)
          .transform(processor)
          .publishOn(cpuLiteScheduler)
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
    } else {
      return publisher -> from(publisher)
          .transform(processor)
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
    }
  }

  protected ScheduledExecutorService getNonBlockingTaskScheduler() {
    return decorateScheduler(getCpuLightScheduler());
  }

  @Override
  public void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {
    final BackPressureReason reason = checkCapacity(event);
    if (reason != null) {
      throw new FromFlowRejectedExecutionException(reason);
    }
  }

  @Override
  public BackPressureReason checkBackpressureEmitting(CoreEvent event) {
    return checkCapacity(event);
  }

  /**
   * Check the capacity of the processing strategy to process events at the moment.
   *
   * @param event the event about to be processed
   * @return true if the event can be accepted for processing
   */
  protected BackPressureReason checkCapacity(CoreEvent event) {
    if (maxConcurrencyEagerCheck) {
      if (inFlightEvents.incrementAndGet() > maxConcurrency) {
        inFlightEvents.decrementAndGet();
        return MAX_CONCURRENCY_EXCEEDED;
      }

      // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like in
      // an async, for instance)
      ((BaseEventContext) event.getContext()).onResponse(inFlightDecrementCallback);
    }

    return null;
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
    stopSchedulersIfNeeded();
  }

  /**
   * Stops all schedulers that should be stopped by this ProcessingStrategy
   *
   * @return whether or not it was needed or not
   */
  protected boolean stopSchedulersIfNeeded() {
    if (cpuLightScheduler != null) {
      cpuLightScheduler.stop();
    }

    return true;
  }

  protected Scheduler getCpuLightScheduler() {
    return cpuLightScheduler;
  }

  protected int getBufferQueueSize() {
    return DEFAULT_BUFFER_SIZE;
  }
}
