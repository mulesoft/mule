/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.getInteger;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.mule.runtime.api.exception.MuleException;
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
 * @since 4.2.0
 */
public class ProactorStreamEmitterProcessingStrategyFactory extends ReactorStreamProcessingStrategyFactory {

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
                                                       () -> muleContext.getSchedulerService()
                                                           .customScheduler(muleContext.getSchedulerBaseConfig()
                                                               .withName(schedulersNamePrefix + ".retrySupport")
                                                               .withMaxConcurrentTasks(CORES)),
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

  static class ProactorStreamEmitterProcessingStrategy extends ProactorStreamProcessingStrategy {

    private static Logger LOGGER = getLogger(ProactorStreamEmitterProcessingStrategy.class);
    private static int SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

    private boolean isThreadLoggingEnabled;

    public ProactorStreamEmitterProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                                   int bufferSize,
                                                   int subscriberCount,
                                                   String waitStrategy,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   Supplier<Scheduler> retrySupportSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency,
                                                   boolean isThreadLoggingEnabled)

    {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier,
            blockingSchedulerSupplier, cpuIntensiveSchedulerSupplier, retrySupportSchedulerSupplier, parallelism, maxConcurrency);
      this.isThreadLoggingEnabled = isThreadLoggingEnabled;
    }

    public ProactorStreamEmitterProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                                   int bufferSize,
                                                   int subscriberCount,
                                                   String waitStrategy,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   Supplier<Scheduler> retrySupportSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency)

    {
      this(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier,
           blockingSchedulerSupplier, cpuIntensiveSchedulerSupplier, retrySupportSchedulerSupplier, parallelism, maxConcurrency,
           false);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
      List<ReactorSink<CoreEvent>> sinks = new ArrayList<>();

      int subscriberCount = maxConcurrency < subscribers ? maxConcurrency : subscribers;
      for (int i = 0; i < subscriberCount; i++) {
        EmitterProcessor<CoreEvent> processor = EmitterProcessor.create(bufferSize);
        processor.doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
            .transform(function).subscribe();
        ReactorSink<CoreEvent> sink = new DefaultReactorSink<>(processor.sink(), () -> {
        }, createOnEventConsumer(), bufferSize);
        sinks.add(sink);
      }

      return new RoundRobinReactorSink<>(sinks);
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency <= subscribers) {
        return pipeline;
      }
      reactor.core.scheduler.Scheduler scheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
      return publisher -> from(publisher).publishOn(scheduler).transform(pipeline);
    }

    @Override
    protected Publisher<CoreEvent> scheduleProcessor(ReactiveProcessor processor, Scheduler processorScheduler, CoreEvent event) {
      return scheduleWithLogging(processor, processorScheduler, event)
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

  static class RoundRobinReactorSink<E> implements AbstractProcessingStrategy.ReactorSink<E> {

    private final List<AbstractProcessingStrategy.ReactorSink<E>> fluxSinks;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinReactorSink(List<AbstractProcessingStrategy.ReactorSink<E>> sinks) {
      this.fluxSinks = sinks;
    }

    @Override
    public void dispose() {
      fluxSinks.stream().forEach(sink -> sink.dispose());
    }

    @Override
    public void accept(CoreEvent event) {
      fluxSinks.get(nextIndex()).accept(event);
    }

    private int nextIndex() {
      return index.getAndUpdate(v -> (v + 1) % fluxSinks.size());
    }

    @Override
    public boolean emit(CoreEvent event) {
      return fluxSinks.get(nextIndex()).emit(event);
    }

    @Override
    public E intoSink(CoreEvent event) {
      return (E) event;
    }
  }

}
