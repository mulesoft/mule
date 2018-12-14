/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.retry.BackoffDelay;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.lang.Integer.getInteger;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.SYSTEM_PROPERTY_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.retry.Retry.onlyIf;

public abstract class ProactorStreamProcessingStrategy
    extends ReactorStreamProcessingStrategyFactory.ReactorStreamProcessingStrategy {

  protected static final int STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD =
      getInteger(SYSTEM_PROPERTY_PREFIX + "STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD", KB.toBytes(16));
  private static Logger LOGGER = getLogger(ProactorStreamProcessingStrategy.class);

  private static int SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

  private final Supplier<Scheduler> blockingSchedulerSupplier;
  private final Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
  private final Supplier<Scheduler> retrySupportSchedulerSupplier;
  private Scheduler blockingScheduler;
  private Scheduler cpuIntensiveScheduler;
  private Scheduler retrySupportScheduler;

  private final AtomicInteger retryingCounter = new AtomicInteger();

  public ProactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                          int bufferSize,
                                          int subscriberCount,
                                          String waitStrategy,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier,
                                          Supplier<Scheduler> blockingSchedulerSupplier,
                                          Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                          Supplier<Scheduler> retrySupportSchedulerSupplier,
                                          int parallelism,
                                          int maxConcurrency,
                                          boolean maxConcurrencyEagerCheck)

  {
    super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier, parallelism,
          maxConcurrency, maxConcurrencyEagerCheck);
    this.blockingSchedulerSupplier = blockingSchedulerSupplier;
    this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
    this.retrySupportSchedulerSupplier = retrySupportSchedulerSupplier;
  }

  @Override
  public void start() throws MuleException {
    super.start();
    this.blockingScheduler = blockingSchedulerSupplier.get();
    this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
    this.retrySupportScheduler = retrySupportSchedulerSupplier.get();
  }

  @Override
  public void stop() throws MuleException {
    if (blockingScheduler != null) {
      blockingScheduler.stop();
    }
    if (cpuIntensiveScheduler != null) {
      cpuIntensiveScheduler.stop();
    }
    if (retrySupportScheduler != null) {
      retrySupportScheduler.stop();
    }
    super.stop();
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

  private ReactiveProcessor proactor(ReactiveProcessor processor, Scheduler scheduler) {
    return publisher -> from(publisher).flatMap(event -> {
      if (processor.getProcessingType() == IO_RW && !scheduleIoRwEvent(event)) {
        // If payload is not a stream o length is < STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD (default 16KB) perform processing on
        // current thread in stead of scheduling using IO pool.
        return just(event)
            .transform(processor)
            .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
      } else {
        return withRetry(scheduleProcessor(processor, scheduler, event)
            .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler)), scheduler);
      }
    }, max(maxConcurrency / (getParallelism() * subscribers), 1));
  }

  protected boolean scheduleIoRwEvent(CoreEvent event) {
    return event.getMessage().getPayload().getDataType().isStreamType()
        && event.getMessage().getPayload().getByteLength().orElse(MAX_VALUE) > STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD;
  }

  protected abstract Flux<CoreEvent> scheduleProcessor(ReactiveProcessor processor, Scheduler processorScheduler,
                                                       CoreEvent event);

  private Flux<CoreEvent> withRetry(Flux<CoreEvent> scheduledFlux, Scheduler processorScheduler) {
    return scheduledFlux.retryWhen(onlyIf(ctx -> {
      final boolean schedulerBusy = isSchedulerBusy(ctx.exception());
      if (schedulerBusy) {
        LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                     processorScheduler.getName(), SCHEDULER_BUSY_RETRY_INTERVAL_MS);
        retryingCounter.incrementAndGet();
      }
      return schedulerBusy;
    }).doOnRetry(ctx -> getRetrySupportScheduler().schedule(() -> {
      // Eventually cleanup the retrying counter for this one. If it is still retrying, the counter will be increased
      // again by the retry mechanism.
      retryingCounter.decrementAndGet();
    }, SCHEDULER_BUSY_RETRY_INTERVAL_MS * 2, MILLISECONDS))
        .backoff(ctx -> new BackoffDelay(ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS)))
        .withBackoffScheduler(fromExecutorService(getCpuLightScheduler())));
  }

  protected Scheduler getBlockingScheduler() {
    return this.blockingScheduler;
  }

  protected Scheduler getCpuIntensiveScheduler() {
    return this.cpuIntensiveScheduler;
  }

  protected Scheduler getRetrySupportScheduler() {
    return this.retrySupportScheduler;
  }

  private final AtomicInteger inFlightEvents = new AtomicInteger();
  private final BiConsumer<CoreEvent, Throwable> IN_FLIGHT_DECREMENT_CALLBACK = (e, t) -> inFlightEvents.decrementAndGet();

  protected final class ProactorSinkWrapper<E> implements ReactorSink<E> {

    private final ReactorSink<E> innerSink;

    protected ProactorSinkWrapper(ReactorSink<E> innerSink) {
      this.innerSink = innerSink;
    }

    @Override
    public final void accept(CoreEvent event) {
      if (!checkCapacity(event)) {
        throw new RejectedExecutionException();
      }

      innerSink.accept(event);
    }

    @Override
    public final boolean emit(CoreEvent event) {
      return checkCapacity(event) && innerSink.emit(event);
    }

    private boolean checkCapacity(CoreEvent event) {
      if (retryingCounter.get() > 0) {
        return false;
      }
      if (maxConcurrencyEagerCheck) {
        if (inFlightEvents.incrementAndGet() > maxConcurrency) {
          inFlightEvents.decrementAndGet();
          return false;
        }

        // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like in
        // an async, for instance)
        ((BaseEventContext) event.getContext()).onResponse(IN_FLIGHT_DECREMENT_CALLBACK);
      }

      return true;
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
