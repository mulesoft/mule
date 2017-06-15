/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
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
    return new TransactionAwareWorkQueueProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(muleContext.getSchedulerBaseConfig().withName(schedulersNamePrefix + "." + BLOCKING.name())
            .withMaxConcurrentTasks(getMaxConcurrency())));
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyClass() {
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
      return new DelegateSink(syncSink, workQueueSink);
    }

    @Override
    protected Consumer<Event> createOnEventConsumer() {
      // Do nothing given event should still be processed when transaction is active
      return event -> {
      };
    }

    @Override
    protected ExecutorService decorateScheduler(Scheduler scheduler) {
      return new ConditionalExecutorServiceDecorator(scheduler, cuurentScheduler -> isTransactionActive());
    }

    final static class DelegateSink implements Sink, Disposable {

      private final Sink syncSink;
      private final Sink workQueueSink;

      public DelegateSink(Sink syncSink, Sink workQueueSink) {
        this.syncSink = syncSink;
        this.workQueueSink = workQueueSink;
      }

      @Override
      public void accept(Event event) {
        if (isTransactionActive()) {
          syncSink.accept(event);
        } else {
          workQueueSink.accept(event);
        }
      }

      @Override
      public void dispose() {
        disposeIfNeeded(syncSink, NOP_LOGGER);
        disposeIfNeeded(workQueueSink, NOP_LOGGER);
      }
    }
  }

}
