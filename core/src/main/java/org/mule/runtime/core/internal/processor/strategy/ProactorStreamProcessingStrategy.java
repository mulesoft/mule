/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.lang.Math.max;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.util.rx.RejectionCallbackExecutorServiceDecorator;
import org.mule.runtime.core.internal.util.rx.RetrySchedulerWrapper;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class ProactorStreamProcessingStrategy extends AbstractReactorStreamProcessingStrategy {

  private static final Logger LOGGER = getLogger(ProactorStreamProcessingStrategy.class);

  private static final long SCHEDULER_BUSY_RETRY_INTERVAL_NS = MILLISECONDS.toNanos(SCHEDULER_BUSY_RETRY_INTERVAL_MS);

  private final Supplier<Scheduler> blockingSchedulerSupplier;
  private final Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
  private Scheduler blockingScheduler;
  private Scheduler cpuIntensiveScheduler;

  protected final AtomicLong lastRetryTimestamp = new AtomicLong(MIN_VALUE);

  private boolean policyMode;

  public ProactorStreamProcessingStrategy(int subscriberCount,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier,
                                          Supplier<Scheduler> blockingSchedulerSupplier,
                                          Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                          int parallelism,
                                          int maxConcurrency,
                                          boolean maxConcurrencyEagerCheck)

  {
    super(subscriberCount, cpuLightSchedulerSupplier, parallelism, maxConcurrency, maxConcurrencyEagerCheck);
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
  protected Scheduler createCpuLightScheduler(Supplier<Scheduler> cpuLightSchedulerSupplier) {
    return new RetrySchedulerWrapper(super.createCpuLightScheduler(cpuLightSchedulerSupplier), SCHEDULER_BUSY_RETRY_INTERVAL_MS);
  }

  @Override
  public void stop() throws MuleException {
    super.stop();
    if (blockingScheduler != null) {
      blockingScheduler.stop();
    }
    if (cpuIntensiveScheduler != null) {
      cpuIntensiveScheduler.stop();
    }
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    if (processor.getProcessingType() == BLOCKING || processor.getProcessingType() == IO_RW) {
      return proactor(processor, blockingScheduler);
    } else if (processor.getProcessingType() == CPU_INTENSIVE) {
      return proactor(processor, cpuIntensiveScheduler);
    } else {
      return super.onProcessor(processor);
    }
  }

  protected ReactiveProcessor proactor(ReactiveProcessor processor, ScheduledExecutorService scheduler) {
    LOGGER.debug("Doing proactor() for {} on {}. maxConcurrency={}, parallelism={}, subscribers={}", processor, scheduler,
                 maxConcurrency, getParallelism(), subscribers);

    final ScheduledExecutorService retryScheduler =
        new RejectionCallbackExecutorServiceDecorator(scheduler, getCpuLightScheduler(),
                                                      () -> onRejected(scheduler),
                                                      () -> lastRetryTimestamp.set(MIN_VALUE),
                                                      ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS));

    if (policyMode) {
      return publisher -> scheduleProcessor(processor, retryScheduler, from(publisher))
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler));
    } else {
      // FlatMap is the way reactor has to do parallel processing. Since this proactor method is used for the processors that are
      // not CPU_LITE, parallelism is wanted when the processor is blocked to do IO or doing long CPU work.
      if (maxConcurrency <= getParallelism() * subscribers) {
        // If no concurrency needed, execute directly on the same Flux
        return publisher -> scheduleProcessor(processor, retryScheduler, from(publisher))
            .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler));
      } else if (maxConcurrency == MAX_VALUE) {
        // For no limit, pass through the no limit meaning to Reactor's flatMap
        return publisher -> from(publisher)
            .flatMap(event -> scheduleProcessor(processor, retryScheduler, Mono.just(event))
                .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler)),
                     MAX_VALUE);
      } else {
        // Otherwise, enforce the concurrency limit from the config,
        return publisher -> from(publisher)
            .flatMap(event -> scheduleProcessor(processor, retryScheduler, Mono.just(event))
                .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler)),
                     max(maxConcurrency / (getParallelism() * subscribers), 1));
      }
    }
  }

  private void onRejected(ScheduledExecutorService scheduler) {
    LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                 (scheduler instanceof Scheduler
                     ? ((Scheduler) scheduler).getName()
                     : scheduler.toString()),
                 SCHEDULER_BUSY_RETRY_INTERVAL_MS);
    lastRetryTimestamp.set(nanoTime());
  }

  // TODO MULE-17079 Remove this policyMode flag.
  public void setPolicyMode(boolean policyMode) {
    this.policyMode = policyMode;
  }

  protected abstract Mono<CoreEvent> scheduleProcessor(ReactiveProcessor processor,
                                                       ScheduledExecutorService processorScheduler,
                                                       Mono<CoreEvent> eventFlux);

  protected abstract Flux<CoreEvent> scheduleProcessor(ReactiveProcessor processor,
                                                       ScheduledExecutorService processorScheduler,
                                                       Flux<CoreEvent> eventFlux);

  protected Scheduler getBlockingScheduler() {
    return this.blockingScheduler;
  }

  protected Scheduler getCpuIntensiveScheduler() {
    return this.cpuIntensiveScheduler;
  }

  private final AtomicInteger inFlightEvents = new AtomicInteger();
  private final AtomicInteger queuedEvents = new AtomicInteger();
  private final BiConsumer<CoreEvent, Throwable> IN_FLIGHT_DECREMENT_CALLBACK = (e, t) -> inFlightEvents.decrementAndGet();
  private final BiConsumer<CoreEvent, Throwable> QUEUED_DECREMENT_CALLBACK = (e, t) -> queuedEvents.decrementAndGet();
  private final LongUnaryOperator LAST_RETRY_TIMESTAMP_CHECK_OPERATOR =
      v -> nanoTime() - v < SCHEDULER_BUSY_RETRY_INTERVAL_NS * 2
          ? v
          : MIN_VALUE;

  /**
   * Check the capacity of the processing strategy to process events at the moment.
   *
   * @param event the event about to be processed
   * @return true if the event can be accepted for processing
   */
  private BackPressureReason checkCapacity(CoreEvent event) {
    if (lastRetryTimestamp.get() != MIN_VALUE) {
      if (lastRetryTimestamp.updateAndGet(LAST_RETRY_TIMESTAMP_CHECK_OPERATOR) != MIN_VALUE) {
        // If there is maxConcurrency value set, honor it and don't buffer here
        if (!maxConcurrencyEagerCheck) {
          // TODO MULE-17265 Make this configurable in the flow
          // This will allow the event to get into the flow, effectively getting into the flow's sink buffer if it cannot be
          // processed right away
          if (queuedEvents.incrementAndGet() > getBufferQueueSize()) {
            queuedEvents.decrementAndGet();
            return REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;
          }

          // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like in
          // an async, for instance)
          ((BaseEventContext) event.getContext()).onResponse(QUEUED_DECREMENT_CALLBACK);
        } else {
          return REQUIRED_SCHEDULER_BUSY;
        }
      }
    }

    if (maxConcurrencyEagerCheck) {
      if (inFlightEvents.incrementAndGet() > maxConcurrency) {
        inFlightEvents.decrementAndGet();
        return MAX_CONCURRENCY_EXCEEDED;
      }

      // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like in
      // an async, for instance)
      ((BaseEventContext) event.getContext()).onResponse(IN_FLIGHT_DECREMENT_CALLBACK);
    }

    return null;
  }

  protected int getBufferQueueSize() {
    return DEFAULT_BUFFER_SIZE;
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

  protected final class ProactorSinkWrapper<E> implements ReactorSink<E> {

    private final ReactorSink<E> innerSink;

    protected ProactorSinkWrapper(ReactorSink<E> innerSink) {
      this.innerSink = innerSink;
    }

    @Override
    public final void accept(CoreEvent event) {
      innerSink.accept(event);
    }

    @Override
    public final BackPressureReason emit(CoreEvent event) {
      return innerSink.emit(event);
    }

    @Override
    public E intoSink(CoreEvent event) {
      return innerSink.intoSink(event);
    }

    @Override
    public final void dispose() {
      innerSink.dispose();
    }
  }

}
