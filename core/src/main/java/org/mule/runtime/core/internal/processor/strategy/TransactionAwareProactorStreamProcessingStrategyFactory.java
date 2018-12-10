/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.ProactorStreamProcessingStrategyFactory.ProactorStreamProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates default processing strategy with same behavior as {@link ProactorStreamProcessingStrategyFactory} apart from the fact
 * it will process synchronously without error when a transaction is active.
 *
 * @since 4.0
 */
public class TransactionAwareProactorStreamProcessingStrategyFactory extends ReactorStreamProcessingStrategyFactory
    implements TransactionAwareProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareProactorStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                                                getBufferSize(),
                                                                getSubscriberCount(),
                                                                getWaitStrategy(), () -> muleContext.getSchedulerService()
                                                                    .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
                                                                        .withName(schedulersNamePrefix + "."
                                                                            + CPU_LITE.name())),
                                                                () -> muleContext.getSchedulerService()
                                                                    .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                                        .withName(schedulersNamePrefix + "."
                                                                            + BLOCKING.name())),
                                                                () -> muleContext.getSchedulerService()
                                                                    .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                                        .withName(schedulersNamePrefix + "."
                                                                            + CPU_INTENSIVE.name())),
                                                                () -> RETRY_SUPPORT_SCHEDULER_PROVIDER.get(muleContext),
                                                                getMaxConcurrency(),
                                                                isMaxConcurrencyEagerCheck());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return TransactionAwareProactorStreamProcessingStrategy.class;
  }

  static class TransactionAwareProactorStreamProcessingStrategy extends ProactorStreamProcessingStrategy {

    TransactionAwareProactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier,
                                                     int bufferSize,
                                                     int subscriberCount,
                                                     String waitStrategy,
                                                     Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                     Supplier<Scheduler> blockingSchedulerSupplier,
                                                     Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                     Supplier<Scheduler> retrySupportSchedulerSupplier,
                                                     int maxConcurrency, boolean maxConcurrencyEagerCheck)

    {
      super(ringBufferSchedulerSupplier, bufferSize, subscriberCount, waitStrategy, cpuLightSchedulerSupplier,
            blockingSchedulerSupplier, cpuIntensiveSchedulerSupplier, retrySupportSchedulerSupplier, CORES, maxConcurrency,
            maxConcurrencyEagerCheck);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      Sink proactorSink = super.createSink(flowConstruct, pipeline);
      Sink syncSink = BLOCKING_PROCESSING_STRATEGY_INSTANCE.createSink(flowConstruct, pipeline);
      return new TransactionalDelegateSink(syncSink, proactorSink);
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
