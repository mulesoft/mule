/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates {@link TransactionAwareWorkQueueStreamProcessingStrategy} instances that de-multiplexes incoming messages using a
 * ring-buffer but instead of processing events using a constrained {@link SchedulerService#cpuLightScheduler()}, or by using the
 * proactor pattern, instead simply performs all processing on a larger work queue pool using a fixed number of threads from the
 * {@link SchedulerService#ioScheduler()}.
 *
 * @since 4.0
 */
public class TransactionAwareWorkQueueStreamProcessingStrategyFactory extends WorkQueueStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareWorkQueueStreamProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1).withWaitAllowed(true)),
                                                                 getBufferSize(),
                                                                 getSubscriberCount(),
                                                                 getWaitStrategy(),
                                                                 () -> muleContext.getSchedulerService()
                                                                     .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                                         .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                                 getMaxConcurrency(),
                                                                 muleContext.getConfiguration().isThreadLoggingEnabled());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return WorkQueueStreamProcessingStrategy.class;
  }

  static class TransactionAwareWorkQueueStreamProcessingStrategy extends WorkQueueStreamProcessingStrategy {

    protected TransactionAwareWorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                                int subscribers,
                                                                String waitStrategy,
                                                                Supplier<Scheduler> blockingSchedulerSupplier,
                                                                int maxConcurrency, boolean isThreadLoggingEnabled) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, blockingSchedulerSupplier, maxConcurrency,
            isThreadLoggingEnabled);
    }

    protected TransactionAwareWorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                                int subscribers,
                                                                String waitStrategy,
                                                                Supplier<Scheduler> blockingSchedulerSupplier,
                                                                int maxConcurrency) {
      this(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, blockingSchedulerSupplier, maxConcurrency, false);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      Sink workQueueSink = super.createSink(flowConstruct, pipeline);
      Sink syncSink = BLOCKING_PROCESSING_STRATEGY_INSTANCE.createSink(flowConstruct, pipeline);
      return new TransactionalDelegateSink(syncSink, workQueueSink);
    }

    @Override
    protected Consumer<CoreEvent> createOnEventConsumer() {
      // Do nothing given event should still be processed when transaction is active
      return event -> {
      };
    }

    @Override
    protected ExecutorService decorateScheduler(Scheduler scheduler) {
      return new ConditionalExecutorServiceDecorator(scheduler, currentScheduler -> isTransactionActive());
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return isTransactionActive() ? BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline) : super.onPipeline(pipeline);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      return isTransactionActive() ? BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor) : super.onProcessor(processor);
    }

  }

}
