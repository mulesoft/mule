/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;

import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * Creates default processing strategy with same behaviuor as {@link ProactorProcessingStrategyFactory} apart from the fact it
 * will process syncronously without errror when a transaction is active.
 */
public class DefaultFlowProcessingStrategyFactory extends ProactorProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new DefaultFlowProcessingStrategy(() -> muleContext.getSchedulerService()
        .cpuLightScheduler(config().withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                             () -> muleContext.getSchedulerService()
                                                 .ioScheduler(config().withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                             () -> muleContext.getSchedulerService()
                                                 .cpuIntensiveScheduler(config()
                                                     .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())),
                                             scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                         MILLISECONDS),
                                             muleContext);
  }

  static class DefaultFlowProcessingStrategy extends ProactorProcessingStrategy {

    public DefaultFlowProcessingStrategy(Supplier<Scheduler> eventLoop, Supplier<Scheduler> io, Supplier<Scheduler> cpu,
                                         Consumer<Scheduler> schedulerStopper,
                                         MuleContext muleContext) {
      super(eventLoop, io, cpu, schedulerStopper, muleContext);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
      Sink proactorSink = super.createSink(flowConstruct, function);
      Sink syncSink = SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE.createSink(flowConstruct, function);
      return new DelegateSink(syncSink, proactorSink);
    }

    @Override
    protected Consumer<Event> createOnEventConsumer() {
      // Do nothing given event should still be processed when transaction is active
      return event -> {
      };
    }

    @Override
    protected Predicate<Scheduler> scheduleOverridePredicate() {
      return scheduler -> isTransactionActive();
    }

    private static class DelegateSink implements Sink, Disposable {

      private final Sink syncSink;
      private final Sink proactorSink;

      public DelegateSink(Sink syncSink, Sink proactorSink) {
        this.syncSink = syncSink;
        this.proactorSink = proactorSink;
      }

      @Override
      public void accept(Event event) {
        if (isTransactionActive()) {
          syncSink.accept(event);
        } else {
          proactorSink.accept(event);
        }
      }

      @Override
      public void dispose() {
        disposeIfNeeded(syncSink, NOP_LOGGER);
        disposeIfNeeded(proactorSink, NOP_LOGGER);
      }
    }
  }

}
