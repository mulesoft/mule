/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.resource.spi.work.WorkManager;

/**
 * This factory's strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single
 * worker thread.
 */
public class AsynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new AsynchronousProcessingStrategy(() -> {
      try {
        return muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }, scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS),
                                              new SynchronousProcessingStrategyFactory().create(muleContext));
  }

  public static class AsynchronousProcessingStrategy implements ProcessingStrategy, Startable, Stoppable {

    protected ProcessingStrategy synchronousProcessingStrategy;

    private Supplier<Scheduler> schedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;

    private Scheduler scheduler;
    private AsyncInterceptingMessageProcessor asyncMessageProcessor;

    public AsynchronousProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper,
                                          ProcessingStrategy synchronousProcessingStrategy) {
      this.schedulerSupplier = schedulerSupplier;
      this.schedulerStopper = schedulerStopper;
      this.synchronousProcessingStrategy = synchronousProcessingStrategy;
    }

    @Override
    public void configureProcessors(List<Processor> processors, MessageProcessorChainBuilder chainBuilder) {
      asyncMessageProcessor = createAsyncMessageProcessor();
      if (processors.size() > 0) {
        chainBuilder.chain(asyncMessageProcessor);
        synchronousProcessingStrategy.configureProcessors(processors, chainBuilder);
      }
    }

    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor() {
      return new AsyncInterceptingMessageProcessor();
    }

    @Override
    public void start() throws MuleException {
      this.scheduler = schedulerSupplier.get();
      asyncMessageProcessor.setScheduler(scheduler);
    }

    @Override
    public void stop() throws MuleException {
      if (scheduler != null) {
        schedulerStopper.accept(scheduler);
        asyncMessageProcessor.setScheduler(null);
      }
    }
  }
}
