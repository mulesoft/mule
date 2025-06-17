/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.PipelineProcessingStrategyReactiveProcessorBuilder.pipelineProcessingStrategyReactiveProcessorFrom;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;

import static java.lang.Long.MIN_VALUE;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.FluxSink.OverflowStrategy.BUFFER;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.RejectionCallbackExecutorServiceDecorator;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.EmitterProcessor;

/**
 * {@link AbstractStreamProcessingStrategyFactory} implementation for Reactor streams using a {@link EmitterProcessor}
 *
 * @since 4.3.0
 */
public class StreamEmitterProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new StreamEmitterProcessingStrategy(getBufferSize(),
                                               getSubscriberCount(),
                                               getFlowDispatchSchedulerSupplier(muleContext, schedulersNamePrefix),
                                               getCpuLightSchedulerSupplier(
                                                                            muleContext,
                                                                            schedulersNamePrefix),
                                               resolveParallelism(),
                                               getMaxConcurrency(),
                                               isMaxConcurrencyEagerCheck(),
                                               () -> muleContext.getConfiguration().getShutdownTimeout());
  }

  @Override
  protected Supplier<Scheduler> getCpuLightSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> muleContext.getSchedulerService()
        .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + "." + CPU_LITE.name()));
  }

  private Supplier<Scheduler> getFlowDispatchSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> {
      SchedulerConfig config = muleContext.getSchedulerBaseConfig().withName(schedulersNamePrefix + ".dispatch");

      if (FLOW_DISPATCH_WORKERS > 0) {
        config = config.withMaxConcurrentTasks(FLOW_DISPATCH_WORKERS);
      }

      return muleContext.getSchedulerService().cpuLightScheduler(config);
    };
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return StreamEmitterProcessingStrategy.class;
  }


  static class StreamEmitterProcessingStrategy extends AbstractReactorStreamProcessingStrategy {

    private static final Logger LOGGER = getLogger(StreamEmitterProcessingStrategy.class);
    private static final String NO_SUBSCRIPTIONS_ACTIVE_FOR_PROCESSOR = "No subscriptions active for processor.";
    private static final long SCHEDULER_BUSY_RETRY_INTERVAL_NS = MILLISECONDS.toNanos(SCHEDULER_BUSY_RETRY_INTERVAL_MS);

    private final int bufferSize;
    private final LazyValue<Scheduler> flowDispatchSchedulerLazy;
    private final AtomicLong lastRetryTimestamp = new AtomicLong(MIN_VALUE);
    private final AtomicInteger queuedEvents = new AtomicInteger();
    private final BiConsumer<CoreEvent, Throwable> queuedDecrementCallback = (e, t) -> queuedEvents.decrementAndGet();
    private final LongUnaryOperator lastRetryTimestampCheckOperator =
        v -> nanoTime() - v < SCHEDULER_BUSY_RETRY_INTERVAL_NS * 2
            ? v
            : MIN_VALUE;

    private final int sinksCount;
    private final Supplier<Long> shutdownTimeoutSupplier;

    // This counter keeps track of how many sinks are created for fluxes that use this processing strategy.
    // Using it, an eager stop of the schedulers is implmented in `stopSchedulersIfNeeded`
    private final AtomicInteger activeSinksCount = new AtomicInteger(0);

    public StreamEmitterProcessingStrategy(int bufferSize,
                                           int subscribers,
                                           Supplier<Scheduler> flowDispatchSchedulerSupplier,
                                           Supplier<Scheduler> cpuLightSchedulerSupplier,
                                           int parallelism,
                                           int maxConcurrency,
                                           boolean maxConcurrencyEagerCheck,
                                           Supplier<Long> shutdownTimeoutSupplier) {
      super(subscribers, cpuLightSchedulerSupplier, parallelism, maxConcurrency, maxConcurrencyEagerCheck);
      this.bufferSize = bufferSize;
      this.flowDispatchSchedulerLazy = new LazyValue<>(flowDispatchSchedulerSupplier);
      this.sinksCount = getSinksCount();
      this.shutdownTimeoutSupplier = shutdownTimeoutSupplier;
    }

    @Override
    public void stop() {
      // Stop the schedulers as early as possible
      if (allSchedulersStopped()) {
        stopSchedulersIfNeeded();
      }
      super.stop();
    }

    @Override
    public void dispose() {
      long startMillis = currentTimeMillis();
      final long shutdownTimeout = shutdownTimeoutSupplier.get();

      while (!allSchedulersStopped() && currentTimeMillis() - startMillis < shutdownTimeout) {
        try {
          sleep(10);
        } catch (InterruptedException e) {
          currentThread().interrupt();
          return;
        }
      }

      if (!allSchedulersStopped()) {
        if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
          throw new IllegalStateException("Schedulers of ProcessingStrategy not stopped before shutdown timeout.");
        } else {
          LOGGER.warn("Schedulers of ProcessingStrategy not stopped before shutdown timeout.");
        }
      }

      // force the schedulers to be stopped.
      final int pendingSinks = activeSinksCount.getAndSet(0);
      super.dispose();

      // All pending threads have been interrupted at this point, so we just need to wait for proper completion
      startMillis = currentTimeMillis();
      while (activeSinksCount.get() >= -pendingSinks && currentTimeMillis() - startMillis < shutdownTimeout) {
        try {
          sleep(10);
        } catch (InterruptedException e) {
          currentThread().interrupt();
          return;
        }
      }
      if (activeSinksCount.get() >= -pendingSinks) {
        if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
          throw new IllegalStateException(
                                          "Completion of ProcessingStrategy sinks not complete/cancelled before shutdown timeout.");
        } else {
          LOGGER.warn("Completion of ProcessingStrategy sinks not complete/cancelled before shutdown timeout.");
        }
      }
    }

    private boolean allSchedulersStopped() {
      return activeSinksCount.get() <= 0;
    }

    @Override
    protected boolean stopSchedulersIfNeeded() {
      // While there are active fluxes, the schedulers cannot be stopped because they are still being used.
      // Moreover, the `complete` of the flux done during shutdown will require those schedulers active to correctly propagate it
      // downstream
      boolean shouldStop = decrementActiveSinks();

      if (shouldStop) {
        try {
          super.stopSchedulersIfNeeded();
        } finally {
          flowDispatchSchedulerLazy.ifComputed(Scheduler::stop);
        }
      }

      return shouldStop;
    }

    private boolean decrementActiveSinks() {
      return activeSinksCount.decrementAndGet() <= 0;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
      List<ReactorSink<CoreEvent>> sinks = new ArrayList<>();
      final int bufferQueueSize = getBufferQueueSize();

      for (int i = 0; i < sinksCount; i++) {
        Latch completionLatch = new Latch();
        EmitterProcessor<CoreEvent> processor = EmitterProcessor.create(bufferQueueSize);
        AtomicReference<Throwable> failedSubscriptionCause = new AtomicReference<>();
        processor.transform(function)
            .subscribe(null, getThrowableConsumer(flowConstruct, completionLatch, failedSubscriptionCause),
                       () -> completionLatch.release());

        if (!processor.hasDownstreams()) {
          throw resolveSubscriptionErrorCause(failedSubscriptionCause);
        }

        ReactorSink<CoreEvent> sink =
            new DefaultReactorSink<>(processor.sink(BUFFER),
                                     prepareDisposeTimestamp -> {
                                       awaitSubscribersCompletion(flowConstruct, shutdownTimeoutSupplier.get(), completionLatch,
                                                                  prepareDisposeTimestamp);
                                       stopSchedulersIfNeeded();
                                     },
                                     onEventConsumer, bufferQueueSize);
        sinks.add(sink);
      }

      activeSinksCount.addAndGet(sinksCount);
      return new RoundRobinReactorSink<>(sinks);
    }

    @Override
    public void registerInternalSink(Publisher<CoreEvent> flux, String sinkRepresentation) {
      from(flux).subscribe(null, e -> {
        LOGGER.atError()
            .setCause(e)
            .log("Exception reached PS subscriber for {}", sinkRepresentation);
        stopSchedulersIfNeeded();
      },
                           () -> stopSchedulersIfNeeded());

      activeSinksCount.incrementAndGet();
    }

    @Override
    public Publisher<CoreEvent> configureInternalPublisher(Publisher<CoreEvent> flux) {
      return from(flux)
          .doAfterTerminate(() -> decrementActiveSinks())
          .doOnSubscribe(s -> activeSinksCount.incrementAndGet());
    }

    @Override
    protected ScheduledExecutorService getNonBlockingTaskScheduler() {
      ScheduledExecutorService scheduler = super.getNonBlockingTaskScheduler();
      return getRetryScheduler(scheduler);
    }

    protected ScheduledExecutorService getRetryScheduler(ScheduledExecutorService scheduler) {
      return new RejectionCallbackExecutorServiceDecorator(scheduler, scheduler,
                                                           () -> onRejected(scheduler),
                                                           () -> lastRetryTimestamp.set(MIN_VALUE),
                                                           ofMillis(SCHEDULER_BUSY_RETRY_INTERVAL_MS));
    }

    protected void onRejected(ScheduledExecutorService scheduler) {
      LOGGER.trace("Shared scheduler {} is busy. Scheduling of the current event will be retried after {}ms.",
                   (scheduler instanceof Scheduler
                       ? ((Scheduler) scheduler).getName()
                       : scheduler.toString()),
                   SCHEDULER_BUSY_RETRY_INTERVAL_MS);
      lastRetryTimestamp.set(nanoTime());
    }

    protected int getSinksCount() {
      int coresLoad = CORES * 2;
      return min(maxConcurrency, coresLoad);
    }

    protected MuleRuntimeException resolveSubscriptionErrorCause(AtomicReference<Throwable> failedSubscriptionCause) {
      MuleRuntimeException exceptionToThrow;
      if (failedSubscriptionCause.get() != null) {
        exceptionToThrow = new MuleRuntimeException(createStaticMessage(NO_SUBSCRIPTIONS_ACTIVE_FOR_PROCESSOR),
                                                    failedSubscriptionCause.get());
      } else {
        exceptionToThrow = new MuleRuntimeException(createStaticMessage(NO_SUBSCRIPTIONS_ACTIVE_FOR_PROCESSOR));
      }
      return exceptionToThrow;
    }

    protected Consumer<Throwable> getThrowableConsumer(FlowConstruct flowConstruct, Latch completionLatch,
                                                       AtomicReference<Throwable> failedSubscriptionCause) {
      return e -> {
        LOGGER.atError()
            .setCause(e)
            .log("Exception reached PS subscriber for flow '{}'", flowConstruct.getName());

        failedSubscriptionCause.set(e);
        completionLatch.release();
      };
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return pipelineProcessingStrategyReactiveProcessorFrom(pipeline, executionClassloader, getArtifactId(muleContext),
                                                             getArtifactType(muleContext))
                                                                 .withScheduler(decorateScheduler(getFlowDispatcherScheduler()))
                                                                 .withProfilingService(getProfilingService())
                                                                 .build();
    }

    @Override
    protected BackPressureReason checkCapacity(CoreEvent event) {
      // are there any retries pending?
      if (lastRetryTimestamp.get() != MIN_VALUE) {
        // did enough time not pass since the last rejection?
        if (lastRetryTimestamp.updateAndGet(lastRetryTimestampCheckOperator) != MIN_VALUE) {
          // If there is maxConcurrency value set, honor it and don't buffer here
          if (!maxConcurrencyEagerCheck) {
            // TODO MULE-17265 Make this configurable in the flow
            // This will allow the event to get into the flow, effectively getting into the flow's sink buffer if it cannot be
            // processed right away
            if (queuedEvents.incrementAndGet() > getBufferQueueSize()) {
              queuedEvents.decrementAndGet();
              return REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;
            }

            // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like
            // in an async, for instance)
            ((BaseEventContext) event.getContext()).onResponse(queuedDecrementCallback);
          } else {
            return REQUIRED_SCHEDULER_BUSY;
          }
        }
      }

      return super.checkCapacity(event);
    }

    protected Scheduler getFlowDispatcherScheduler() {
      return flowDispatchSchedulerLazy.get();
    }

    @Override
    protected int getBufferQueueSize() {
      return bufferSize / sinksCount;
    }

    static class RoundRobinReactorSink<E> implements AbstractProcessingStrategy.ReactorSink<E> {

      private final List<AbstractProcessingStrategy.ReactorSink<E>> fluxSinks;
      private final AtomicInteger index = new AtomicInteger(0);
      // Saving update function to avoid creating the lambda every time
      private final IntUnaryOperator update;

      public RoundRobinReactorSink(List<AbstractProcessingStrategy.ReactorSink<E>> sinks) {
        this.fluxSinks = sinks;
        this.update = value -> (value + 1) % fluxSinks.size();
      }

      @Override
      public void prepareDispose() {
        fluxSinks.stream().forEach(sink -> sink.prepareDispose());
      }

      @Override
      public void dispose() {
        fluxSinks.stream().forEach(sink -> sink.prepareDispose());
        fluxSinks.stream().forEach(sink -> sink.dispose());
      }

      @Override
      public void accept(CoreEvent event) {
        fluxSinks.get(nextIndex()).accept(event);
      }

      private int nextIndex() {
        return index.getAndUpdate(update);
      }

      @Override
      public BackPressureReason emit(CoreEvent event) {
        return fluxSinks.get(nextIndex()).emit(event);
      }

      @Override
      public E intoSink(CoreEvent event) {
        return (E) event;
      }
    }
  }
}
