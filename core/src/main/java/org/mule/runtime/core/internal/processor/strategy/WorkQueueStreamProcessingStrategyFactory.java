/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ofMillis;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger.THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory.WaitStrategy.valueOf;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.FluxSink.OverflowStrategy.BUFFER;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.util.concurrent.WaitStrategy.blocking;
import static reactor.util.concurrent.WaitStrategy.busySpin;
import static reactor.util.concurrent.WaitStrategy.liteBlocking;
import static reactor.util.concurrent.WaitStrategy.parking;
import static reactor.util.concurrent.WaitStrategy.phasedOffLiteLock;
import static reactor.util.concurrent.WaitStrategy.sleeping;
import static reactor.util.concurrent.WaitStrategy.yielding;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.context.thread.notification.ThreadLoggingExecutorServiceDecorator;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.publisher.Mono;
import reactor.core.publisher.WorkQueueProcessor;

/**
 * Creates {@link WorkQueueStreamProcessingStrategy} instances that de-multiplexes incoming messages using a ring-buffer but
 * instead of processing events using a constrained {@link SchedulerService#cpuLightScheduler()}, or by using the proactor
 * pattern, instead simply performs all processing on a larger work queue pool using a fixed number of threads from the
 * {@link SchedulerService#ioScheduler()}.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueStreamProcessingStrategyFactory extends AbstractStreamWorkQueueProcessingStrategyFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkQueueStreamProcessingStrategyFactory.class);

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                                 getBufferSize(),
                                                 getSubscriberCount(),
                                                 getWaitStrategy(),
                                                 () -> muleContext.getSchedulerService()
                                                     .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                         .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                 getMaxConcurrency(), isMaxConcurrencyEagerCheck(),
                                                 muleContext.getConfiguration().isThreadLoggingEnabled());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return WorkQueueStreamProcessingStrategy.class;
  }

  static class WorkQueueStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

    private final int bufferSize;
    private final Supplier<Scheduler> ringBufferSchedulerSupplier;
    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private final WaitStrategy waitStrategy;
    private Scheduler blockingScheduler;
    private final List<Sink> sinkList = new ArrayList<>();
    private final boolean isThreadLoggingEnabled;

    protected WorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                int subscribers,
                                                String waitStrategy, Supplier<Scheduler> blockingSchedulerSupplier,
                                                int maxConcurrency, boolean maxConcurrencyEagerCheck,
                                                boolean isThreadLoggingEnabled) {
      super(subscribers, maxConcurrency, maxConcurrencyEagerCheck);
      this.bufferSize = requireNonNull(bufferSize);
      this.ringBufferSchedulerSupplier = requireNonNull(ringBufferSchedulerSupplier);
      this.blockingSchedulerSupplier = requireNonNull(blockingSchedulerSupplier);
      this.waitStrategy = valueOf(waitStrategy);
      this.isThreadLoggingEnabled = isThreadLoggingEnabled;
    }

    protected WorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                int subscribers,
                                                String waitStrategy, Supplier<Scheduler> blockingSchedulerSupplier,
                                                int maxConcurrency, boolean maxConcurrencyEagerCheck) {
      this(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, blockingSchedulerSupplier, maxConcurrency,
           maxConcurrencyEagerCheck, false);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
      final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
      WorkQueueProcessor<EventWrapper> processor =
          WorkQueueProcessor.<EventWrapper>builder().executor(ringBufferSchedulerSupplier.get()).bufferSize(bufferSize)
              .waitStrategy(waitStrategy.getReactorWaitStrategy()).build();
      int subscriberCount = maxConcurrency < subscribers ? maxConcurrency : subscribers;
      CountDownLatch completionLatch = new CountDownLatch(subscriberCount);
      for (int i = 0; i < subscriberCount; i++) {
        processor
            .doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader))
            .map(EventWrapper::getWrappedEvent)
            .transform(function)
            .subscribe(null, e -> completionLatch.countDown(), completionLatch::countDown);
      }
      return buildSink(processor.sink(BUFFER), () -> {
        long start = currentTimeMillis();
        if (!processor.awaitAndShutdown(ofMillis(shutdownTimeout))) {
          LOGGER.warn("WorkQueueProcessor of ProcessingStrategy for flow '{}' not shutDown in {} ms. Forcing shutdown...",
                      flowConstruct.getName(), shutdownTimeout);
          processor.forceShutdown();
        }
        awaitSubscribersCompletion(flowConstruct, shutdownTimeout, completionLatch, start);
      }, createOnEventConsumer(), bufferSize);
    }

    protected <E> ReactorSink<E> buildSink(FluxSink<E> fluxSink, reactor.core.Disposable disposable,
                                           Consumer<CoreEvent> onEventConsumer, int bufferSize) {
      return new DefaultReactorSink(fluxSink, disposable, onEventConsumer, bufferSize) {

        @Override
        public EventWrapper intoSink(CoreEvent event) {
          return new EventWrapper(event);
        }
      };
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency > subscribers) {
        if (isThreadLoggingEnabled) {
          return publisher -> from(publisher).flatMap(event -> Mono.subscriberContext()
              .flatMap(ctx -> Mono.just(event).transform(pipeline)
                  .subscribeOn(fromExecutorService(new ThreadLoggingExecutorServiceDecorator(ctx
                      .getOrEmpty(THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY), decorateScheduler(blockingScheduler),
                                                                                             event.getContext().getId())))));
        } else {
          return publisher -> from(publisher)
              .flatMap(event -> just(event).transform(pipeline)
                  .subscribeOn(fromExecutorService(decorateScheduler(blockingScheduler)))
                  .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, blockingScheduler)),
                       maxConcurrency);
        }
      } else {
        return super.onPipeline(pipeline);
      }
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor)
            .publishOn(fromExecutorService(decorateScheduler(blockingScheduler)));
      } else {
        return super.onProcessor(processor);
      }
    }

    @Override
    public void start() throws MuleException {
      this.blockingScheduler = blockingSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      sinkList.stream().filter(sink -> sink instanceof Disposable).forEach(sink -> ((Disposable) sink).dispose());
      if (blockingScheduler != null) {
        blockingScheduler.stop();
      }
    }

  }

  private static final class EventWrapper {

    CoreEvent wrappedEvent;

    public EventWrapper(CoreEvent event) {
      this.wrappedEvent = event;
      ((BaseEventContext) (wrappedEvent.getContext())).getRootContext().onTerminated((e, t) -> wrappedEvent = null);
    }

    public CoreEvent getWrappedEvent() {
      return wrappedEvent;
    }
  }

  protected enum WaitStrategy {
    BLOCKING(blocking()),

    LITE_BLOCKING(liteBlocking()),

    SLEEPING(sleeping()),

    BUSY_SPIN(busySpin()),

    YIELDING(yielding()),

    PARKING(parking()),

    PHASED(phasedOffLiteLock(200, 100, MILLISECONDS));

    private final reactor.util.concurrent.WaitStrategy reactorWaitStrategy;

    WaitStrategy(reactor.util.concurrent.WaitStrategy reactorWaitStrategy) {
      this.reactorWaitStrategy = reactorWaitStrategy;
    }

    reactor.util.concurrent.WaitStrategy getReactorWaitStrategy() {
      return reactorWaitStrategy;
    }
  }

}
