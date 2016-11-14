/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO operations.
 *
 * @since 3.7
 */
public class LegacyNonBlockingProcessingStrategyFactory extends LegacyAsynchronousProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new LegacyNonBlockingProcessingStrategy(() -> muleContext.getSchedulerService().ioScheduler(),
                                                   scheduler -> scheduler
                                                       .stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS));
  }

  public static class LegacyNonBlockingProcessingStrategy implements ProcessingStrategy, Startable, Stoppable {

    private Supplier<Scheduler> schedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;
    private Scheduler scheduler;

    public LegacyNonBlockingProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper) {
      this.schedulerSupplier = schedulerSupplier;
      this.schedulerStopper = schedulerStopper;
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor processor,
                                                                    Function<Publisher<Event>, Publisher<Event>> processorFunction) {
      if (processor instanceof NonBlockingMessageProcessor) {
        return publisher -> from(publisher).transform(processorFunction).publishOn(fromExecutorService(scheduler));
      } else {
        return publisher -> from(publisher).transform(processorFunction);
      }
    }

    @Override
    public void start() throws MuleException {
      this.scheduler = schedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (scheduler != null) {
        schedulerStopper.accept(scheduler);
      }
    }
  }
}
