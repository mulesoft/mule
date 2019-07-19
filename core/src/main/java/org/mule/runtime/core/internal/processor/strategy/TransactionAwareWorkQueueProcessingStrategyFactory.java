/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates default processing strategy with same behavior as {@link WorkQueueProcessingStrategyFactory} apart from the fact it
 * will process synchronously without error when a transaction is active.
 */
public class TransactionAwareWorkQueueProcessingStrategyFactory extends WorkQueueProcessingStrategyFactory
    implements TransactionAwareProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    SchedulerConfig schedulerConfig = muleContext.getSchedulerBaseConfig().withName(schedulersNamePrefix + "." + BLOCKING.name());
    if (getMaxConcurrency() != MAX_VALUE) {
      schedulerConfig = schedulerConfig.withMaxConcurrentTasks(getMaxConcurrency());
    }
    SchedulerConfig finalSchedulerConfig = schedulerConfig;
    return new TransactionAwareWorkQueueProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(finalSchedulerConfig));
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return TransactionAwareWorkQueueProcessingStrategy.class;
  }

  static class TransactionAwareWorkQueueProcessingStrategy extends WorkQueueProcessingStrategy {

    protected TransactionAwareWorkQueueProcessingStrategy(Supplier<Scheduler> ioSchedulerSupplier) {
      super(ioSchedulerSupplier);
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
    protected ScheduledExecutorService decorateScheduler(ScheduledExecutorService scheduler) {
      return new ConditionalExecutorServiceDecorator(super.decorateScheduler(scheduler),
                                                     currentScheduler -> isTransactionActive());
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
