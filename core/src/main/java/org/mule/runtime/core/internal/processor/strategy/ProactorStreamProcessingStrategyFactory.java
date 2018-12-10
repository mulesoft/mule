/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.getLong;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.retry.Retry.onlyIf;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.retry.BackoffDelay;

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

  protected static final long STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD =
      getLong(SYSTEM_PROPERTY_PREFIX + "STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD", KB.toBytes(16));

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                                getBufferSize(),
                                                getSubscriberCount(),
                                                getWaitStrategy(),
                                                getCpuLightSchedulerSupplier(muleContext, schedulersNamePrefix),
                                                () -> muleContext.getSchedulerService()
                                                    .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                        .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                () -> muleContext.getSchedulerService()
                                                    .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                        .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                                () -> RETRY_SUPPORT_SCHEDULER_PROVIDER.get(muleContext),
                                                resolveParallelism(),
                                                getMaxConcurrency(),
                                                isMaxConcurrencyEagerCheck());
  }

  @Override
  protected int resolveParallelism() {
    if (getMaxConcurrency() == Integer.MAX_VALUE) {
      return max(CORES / getSubscriberCount(), 1);
    } else {
      // Resolve maximum factor of max concurrency that is less than number of cores in order to respect maxConcurrency more
      // closely.
      return min(CORES, maxFactor(Float.max((float) getMaxConcurrency() / getSubscriberCount(), 1)));
    }
  }

  private int maxFactor(float test) {
    if (test % 0 == 0) {
      for (int i = CORES; i > 1; i--)
        if (test % i == 0) {
          return i;
        }
    }
    return 1;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ProactorStreamProcessingStrategy.class;
  }

  static class ProactorStreamProcessingStrategy extends ReactorStreamProcessingStrategy {

    private final AtomicInteger inFlightEvents = new AtomicInteger();
    private final BiConsumer<CoreEvent, Throwable> IN_FLIGHT_DECREMENT_CALLBACK = (e, t) -> inFlightEvents.decrementAndGet();

    private final class ProactorSinkWrapper<E> implements ReactorSink<E> {

      private final ReactorSink<E> innerSink;

      private ProactorSinkWrapper(ReactorSink<E> innerSink) {
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
                                            int maxConcurrency, boolean maxConcurrencyEagerCheck)

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
      reactor.core.scheduler.Scheduler publishOnScheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));

      return publisher -> from(publisher).flatMap(event -> {
        if (processor.getProcessingType() == IO_RW && !scheduleIoRwEvent(event)) {
          // If payload is not a stream o length is < STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD (default 16KB) perform processing on
          // current thread in stead of scheduling using IO pool.
          return just(event).transform(processor)
              .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
        } else {
          return scheduleProcessor(processor, publishOnScheduler, scheduler, event);
        }
      }, max(maxConcurrency / (getParallelism() * subscribers), 1));
    }

    private boolean scheduleIoRwEvent(CoreEvent event) {
      return event.getMessage().getPayload().getDataType().isStreamType()
          && event.getMessage().getPayload().getByteLength().orElse(MAX_VALUE) > STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD;
    }

    private Publisher<CoreEvent> scheduleProcessor(ReactiveProcessor processor,
                                                   reactor.core.scheduler.Scheduler eventLoopScheduler,
                                                   Scheduler processorScheduler, CoreEvent event) {
      return just(event)
          .transform(processor)
          .publishOn(eventLoopScheduler)
          .subscribeOn(fromExecutorService(decorateScheduler(processorScheduler)))
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, processorScheduler))
          .retryWhen(onlyIf(ctx -> {
            final boolean schedulerBusy = isSchedulerBusy(ctx.exception());
            if (schedulerBusy) {
              LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                           processorScheduler.getName(), SCHEDULER_BUSY_RETRY_INTERVAL_MS);

              retryingCounter.incrementAndGet();
            }
            return schedulerBusy;
          })
              .doOnRetry(ctx -> {
                retrySupportScheduler.schedule(() -> {
                  // Eventually cleanup the retrying counter for this one. If it is still retrying, the counter will be increased
                  // again by the retry mechanism.
                  retryingCounter.decrementAndGet();
                }, SCHEDULER_BUSY_RETRY_INTERVAL_MS * 2, MILLISECONDS);
              })
              .backoff(ctx -> new BackoffDelay(ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS)))
              .withBackoffScheduler(fromExecutorService(getCpuLightScheduler())));
    }

    @Override
    protected <E> ReactorSink<E> buildSink(FluxSink<E> fluxSink, Disposable disposable, Consumer<CoreEvent> onEventConsumer,
                                           int bufferSize) {
      return new ProactorSinkWrapper<E>(super.buildSink(fluxSink, disposable, onEventConsumer, bufferSize));
    }
  }

}
