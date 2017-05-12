/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;
import org.mule.runtime.core.processor.strategy.ProactorStreamProcessingStrategyFactory.ProactorStreamProcessingStrategy;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates default processing strategy with same behavior as {@link ProactorStreamProcessingStrategyFactory} apart from the fact
 * it will process synchronously without error when a transaction is active.
 *
 * @since 4.0
 */
public class DefaultStreamProcessingStrategyFactory extends ReactorStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    if (getMaxConcurrency() == 1) {
      return new ReactorProcessingStrategyFactory().create(muleContext, schedulersNamePrefix);
    } else {
      return new RingBufferDefaultProcessingStrategy(() -> muleContext.getSchedulerService()
          .customScheduler(muleContext.getSchedulerBaseConfig()
              .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
              .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                                     getBufferSize(),
                                                     getSubscriberCount(),
                                                     getWaitStrategy(), () -> muleContext.getSchedulerService()
                                                         .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
                                                             .withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                                     () -> muleContext.getSchedulerService()
                                                         .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                             .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                     () -> muleContext.getSchedulerService()
                                                         .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                             .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                                     getMaxConcurrency());
    }
  }

  static class RingBufferDefaultProcessingStrategy extends ProactorStreamProcessingStrategy {

    public RingBufferDefaultProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                               int bufferSize,
                                               int subscriberCount,
                                               String waitStrategy,
                                               Supplier<Scheduler> cpuLightSchedulerSupplier,
                                               Supplier<Scheduler> blockingSchedulerSupplier,
                                               Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                               int maxConcurrency)

    {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier,
            blockingSchedulerSupplier, cpuIntensiveSchedulerSupplier, maxConcurrency);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      Sink proactorSink = super.createSink(flowConstruct, pipeline);
      Sink syncSink = BLOCKING_PROCESSING_STRATEGY_INSTANCE.createSink(flowConstruct, pipeline);
      return new DefaultFlowProcessingStrategyFactory.DefaultFlowProcessingStrategy.DelegateSink(syncSink, proactorSink);
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
  }

}
