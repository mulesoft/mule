/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.getInteger;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.sin;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger.THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.retry.Retry.onlyIf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.context.thread.notification.ThreadLoggingExecutorServiceDecorator;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
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
public class ProactorStreamEmitterProcessingStrategyFactory extends ReactorStreamProcessingStrategyFactory {

  protected static final int STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD =
      getInteger(SYSTEM_PROPERTY_PREFIX + "STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD", KB.toBytes(16));

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorStreamEmitterProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
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
                                                       resolveParallelism(),
                                                       getMaxConcurrency(),
                                                       muleContext.getConfiguration().isThreadLoggingEnabled());
  }

  @Override
  protected int resolveParallelism() {
    return Integer.max(CORES, getMaxConcurrency());
  }

  @Override
  protected final int getSubscriberCount() {
    return 1;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ProactorStreamEmitterProcessingStrategy.class;
  }

  static class ProactorStreamEmitterProcessingStrategy extends ReactorStreamProcessingStrategy {

    private static Logger LOGGER = getLogger(ProactorStreamEmitterProcessingStrategy.class);
    private static int SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

    private Supplier<Scheduler> blockingSchedulerSupplier;
    private Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;
    private boolean isThreadLoggingEnabled;

    public ProactorStreamEmitterProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                                   int bufferSize,
                                                   int subscriberCount,
                                                   String waitStrategy,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency,
                                                   boolean isThreadLoggingEnabled)

    {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier, parallelism,
            maxConcurrency);
      this.blockingSchedulerSupplier = blockingSchedulerSupplier;
      this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
      this.isThreadLoggingEnabled = isThreadLoggingEnabled;
    }

    public ProactorStreamEmitterProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                                   int bufferSize,
                                                   int subscriberCount,
                                                   String waitStrategy,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency)

    {
      this(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier,
           blockingSchedulerSupplier, cpuIntensiveSchedulerSupplier, parallelism, maxConcurrency, false);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
      List<FluxSink<CoreEvent>> sinks = new ArrayList<>();

      int subscriberCount = maxConcurrency < subscribers ? maxConcurrency : subscribers;
      for (int i = 0; i < subscriberCount; i++) {
        EmitterProcessor<CoreEvent> processor = EmitterProcessor.create(bufferSize);
        processor.doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
            .transform(function).subscribe();
        sinks.add(processor.sink());
      }

      return new RoundRobinReactorSink<>(sinks, () -> {
      }, createOnEventConsumer(), bufferSize);
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
          return scheduleProcessor(processor, scheduler, event);
        }
      }, max(maxConcurrency / (getParallelism() * subscribers), 1));
    }

    private boolean scheduleIoRwEvent(CoreEvent event) {
      return event.getMessage().getPayload().getDataType().isStreamType()
          && event.getMessage().getPayload().getByteLength().orElse(MAX_VALUE) > STREAM_PAYLOAD_BLOCKING_IO_THRESHOLD;
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency <= subscribers) {
        return pipeline;
      }
      reactor.core.scheduler.Scheduler scheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
      return publisher -> from(publisher).publishOn(scheduler).transform(pipeline);
    }

    private Publisher<CoreEvent> scheduleProcessor(ReactiveProcessor processor, Scheduler processorScheduler, CoreEvent event) {

      return just(event)
          .transform(processor)
          .subscribeOn(fromExecutorService(decorateScheduler(processorScheduler)))
          .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, processorScheduler))
          .doOnError(RejectedExecutionException.class,
                     throwable -> LOGGER.trace("Shared scheduler " + processorScheduler.getName()
                         + " is busy.  Scheduling of the current event will be retried after " + SCHEDULER_BUSY_RETRY_INTERVAL_MS
                         + "ms."))
          .retryWhen(onlyIf(ctx -> RejectedExecutionException.class.isAssignableFrom(unwrap(ctx.exception()).getClass()))
              .backoff(ctx -> new BackoffDelay(ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS), ZERO, ZERO))
              .withBackoffScheduler(fromExecutorService(getCpuLightScheduler())));
    }

    private Flux<CoreEvent> scheduleWithLogging(ReactiveProcessor processor, Scheduler processorScheduler, CoreEvent event) {
      if (isThreadLoggingEnabled) {
        return just(event)
            .flatMap(e -> subscriberContext()
                .flatMap(ctx -> Mono.just(e).transform(processor)
                    .subscribeOn(fromExecutorService(new ThreadLoggingExecutorServiceDecorator(ctx
                        .getOrEmpty(THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY), decorateScheduler(processorScheduler),
                                                                                               e.getContext().getId())))));
      } else {
        return just(event)
            .transform(processor)
            .subscribeOn(fromExecutorService(decorateScheduler(processorScheduler)));
      }
    }

  }

  static class RoundRobinReactorSink<E> implements Sink, Disposable {

    private final List<FluxSink<E>> fluxSinks;
    private final reactor.core.Disposable disposable;
    private final Consumer onEventConsumer;
    private final int bufferSize;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinReactorSink(List<FluxSink<E>> sinks, reactor.core.Disposable disposable,
                                 Consumer<CoreEvent> onEventConsumer, int bufferSize) {
      this.fluxSinks = sinks;
      this.disposable = disposable;
      this.onEventConsumer = onEventConsumer;
      this.bufferSize = bufferSize;
    }

    @Override
    public void dispose() {
      fluxSinks.stream().forEach(sink -> sink.complete());
      disposable.dispose();
    }

    @Override
    public void accept(CoreEvent event) {
      onEventConsumer.accept(event);
      fluxSinks.get(nextIndex()).next(intoSink(event));
    }

    private int nextIndex() {
      return index.getAndUpdate(v -> (v + 1) % fluxSinks.size());
    }

    protected E intoSink(CoreEvent event) {
      return (E) event;
    }

    @Override
    public boolean emit(CoreEvent event) {
      onEventConsumer.accept(event);
      // Optimization to avoid using synchronized block for all emissions.
      // See: https://github.com/reactor/reactor-core/issues/1037
      FluxSink<E> sink = fluxSinks.get(nextIndex());
      long remainingCapacity = sink.requestedFromDownstream();
      if (remainingCapacity == 0) {
        return false;
      } else if (remainingCapacity > (bufferSize > CORES * 4 ? CORES : 0)) {
        // If there is sufficient room in buffer to significantly reduce change of concurrent emission when buffer is full then
        // emit without synchronized block.
        sink.next(intoSink(event));
        return true;
      } else {
        // If there is very little room in buffer also emit but synchronized.
        synchronized (sink) {
          if (remainingCapacity > 0) {
            sink.next(intoSink(event));
            return true;
          } else {
            return false;
          }
        }
      }
    }
  }

}
