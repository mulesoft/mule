/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger.THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.context.thread.notification.ThreadLoggingExecutorServiceDecorator;
import org.mule.runtime.core.internal.processor.strategy.StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.RetrySchedulerWrapper;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Creates {@link AsyncProcessingStrategyFactory} instance that implements the proactor pattern by
 * de-multiplexing incoming events onto a multiple emitter using the {@link SchedulerService#cpuLightScheduler()} to process these
 * events from each emitter. In contrast to the {@link AbstractStreamProcessingStrategyFactory} the proactor pattern treats
 * {@link ReactiveProcessor.ProcessingType#CPU_INTENSIVE} and {@link ReactiveProcessor.ProcessingType#BLOCKING} processors differently and schedules there execution
 * on dedicated {@link SchedulerService#cpuIntensiveScheduler()} and {@link SchedulerService#ioScheduler()} ()} schedulers.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.2.0
 */
public class ProactorStreamEmitterProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    Supplier<Scheduler> cpuLightSchedulerSupplier = getCpuLightSchedulerSupplier(muleContext, schedulersNamePrefix);
    return new ProactorStreamEmitterProcessingStrategy(getBufferSize(),
                                                       getSubscriberCount(),
                                                       cpuLightSchedulerSupplier,
                                                       cpuLightSchedulerSupplier,
                                                       () -> muleContext.getSchedulerService()
                                                           .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                               .withName(
                                                                         schedulersNamePrefix + "." + BLOCKING.name())),
                                                       () -> muleContext.getSchedulerService()
                                                           .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                               .withName(schedulersNamePrefix + "."
                                                                   + CPU_INTENSIVE.name())),
                                                       resolveParallelism(),
                                                       getMaxConcurrency(),
                                                       isMaxConcurrencyEagerCheck(),
                                                       muleContext.getConfiguration().isThreadLoggingEnabled());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ProactorStreamEmitterProcessingStrategy.class;
  }

  static class ProactorStreamEmitterProcessingStrategy extends StreamEmitterProcessingStrategy {

    private static final Logger LOGGER = getLogger(ProactorStreamEmitterProcessingStrategy.class);

    private final boolean isThreadLoggingEnabled;
    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private final Supplier<Scheduler> cpuIntensiveSchedulerSupplier;

    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;

    public ProactorStreamEmitterProcessingStrategy(int bufferSize,
                                                   int subscriberCount,
                                                   Supplier<Scheduler> flowDispatchSchedulerSupplier,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency,
                                                   boolean maxConcurrencyEagerCheck,
                                                   boolean isThreadLoggingEnabled) {
      super(bufferSize, subscriberCount, flowDispatchSchedulerSupplier, cpuLightSchedulerSupplier, parallelism, maxConcurrency,
            maxConcurrencyEagerCheck);
      this.blockingSchedulerSupplier = blockingSchedulerSupplier;
      this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
      this.isThreadLoggingEnabled = isThreadLoggingEnabled;
    }

    @Override
    public void start() throws MuleException {
      super.start();
      this.blockingScheduler = blockingSchedulerSupplier.get();
      this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
    }

    @Override
    protected int getSinksCount() {
      return maxConcurrency < CORES ? maxConcurrency : CORES;
    }

    @Override
    protected Scheduler createCpuLightScheduler(Supplier<Scheduler> cpuLightSchedulerSupplier) {
      return new RetrySchedulerWrapper(super.createCpuLightScheduler(cpuLightSchedulerSupplier),
                                       SCHEDULER_BUSY_RETRY_INTERVAL_MS);
    }

    @Override
    protected boolean stopSchedulersIfNeeded() {
      if (super.stopSchedulersIfNeeded()) {

        if (blockingScheduler != null) {
          blockingScheduler.stop();
        }
        if (cpuIntensiveScheduler != null) {
          cpuIntensiveScheduler.stop();
        }

        return true;
      }

      return false;
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

      final ScheduledExecutorService retryScheduler = getRetryScheduler(scheduler);

      // FlatMap is the way reactor has to do parallel processing. Since this proactor method is used for the processors that are
      // not CPU_LITE, parallelism is wanted when the processor is blocked to do IO or doing long CPU work.
      if (maxConcurrency == 1) {
        // If no concurrency needed, execute directly on the same Flux
        return publisher -> scheduleProcessor(processor, retryScheduler, from(publisher))
            .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler));
      } else if (maxConcurrency == MAX_VALUE) {
        if (processor instanceof OperationInnerProcessor
            && ((OperationInnerProcessor) processor).isAsync()) {
          // For no limit, the java SDK already does a flatMap internally, so no need to do an additional one here
          return publisher -> scheduleProcessor(processor, retryScheduler, from(publisher))
              .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler));
        } else {
          // For no limit, pass through the no limit meaning to Reactor's flatMap
          return publisher -> from(publisher)
              .flatMap(event -> scheduleProcessor(processor, retryScheduler, Mono.just(event))
                  .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler)),
                       MAX_VALUE);
        }
      } else {
        // Otherwise, enforce the concurrency limit from the config,
        return publisher -> from(publisher)
            .flatMap(event -> scheduleProcessor(processor, retryScheduler, Mono.just(event))
                .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, scheduler)),
                     max(maxConcurrency / (getParallelism() * subscribers), 1));
      }
    }

    private Mono<CoreEvent> scheduleProcessor(ReactiveProcessor processor, ScheduledExecutorService processorScheduler,
                                              Mono<CoreEvent> eventFlux) {
      return scheduleWithLogging(processor, processorScheduler, eventFlux);
    }

    private Flux<CoreEvent> scheduleProcessor(ReactiveProcessor processor, ScheduledExecutorService processorScheduler,
                                              Flux<CoreEvent> eventFlux) {
      return scheduleWithLogging(processor, processorScheduler, eventFlux);
    }

    private Mono<CoreEvent> scheduleWithLogging(ReactiveProcessor processor, ScheduledExecutorService processorScheduler,
                                                Mono<CoreEvent> eventFlux) {
      if (isThreadLoggingEnabled) {
        return Mono.from(eventFlux)
            .flatMap(e -> subscriberContext()
                .flatMap(ctx -> Mono.just(e).transform(processor)
                    .subscribeOn(fromExecutorService(new ThreadLoggingExecutorServiceDecorator(ctx
                        .getOrEmpty(
                                    THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY),
                                                                                               decorateScheduler(
                                                                                                                 processorScheduler),
                                                                                               e.getContext().getId())))));
      } else {
        return Mono.from(eventFlux)
            .publishOn(fromExecutorService(decorateScheduler(processorScheduler)))
            .transform(processor);
      }
    }

    private Flux<CoreEvent> scheduleWithLogging(ReactiveProcessor processor, ScheduledExecutorService processorScheduler,
                                                Flux<CoreEvent> eventFlux) {
      if (isThreadLoggingEnabled) {
        return Flux.from(eventFlux)
            .flatMap(e -> subscriberContext()
                .flatMap(ctx -> Mono.just(e).transform(processor)
                    .subscribeOn(fromExecutorService(new ThreadLoggingExecutorServiceDecorator(ctx
                        .getOrEmpty(
                                    THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY),
                                                                                               decorateScheduler(
                                                                                                                 processorScheduler),
                                                                                               e.getContext().getId())))));
      } else {
        return Flux.from(eventFlux)
            .publishOn(fromExecutorService(decorateScheduler(processorScheduler)))
            .transform(processor);
      }
    }

    @Override
    protected Scheduler getFlowDispatcherScheduler() {
      return getCpuLightScheduler();
    }
  }
}
