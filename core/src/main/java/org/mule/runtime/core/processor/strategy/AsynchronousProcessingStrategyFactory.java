/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;

import java.util.List;

import javax.resource.spi.work.WorkManager;

/**
 * This factory's strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single
 * worker thread.
 */
public class AsynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create() {
    return new AsynchronousProcessingStrategy();
  }

  public static class AsynchronousProcessingStrategy implements ProcessingStrategy, Startable, Stoppable {

    private SchedulerService schedulerService;
    private Scheduler scheduler;
    private MuleContext muleContext;
    private AsyncInterceptingMessageProcessor asyncMessageProcessor;

    protected ProcessingStrategy synchronousProcessingStrategy = new SynchronousProcessingStrategyFactory().create();

    @Override
    public void configureProcessors(List<Processor> processors, SchedulerService schedulerService,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      this.schedulerService = schedulerService;
      this.muleContext = muleContext;
      asyncMessageProcessor = createAsyncMessageProcessor();
      if (processors.size() > 0) {
        chainBuilder.chain(asyncMessageProcessor);
        synchronousProcessingStrategy.configureProcessors(processors, schedulerService, chainBuilder, muleContext);
      }
    }

    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor() {
      return new AsyncInterceptingMessageProcessor();
    }

    @Override
    public void start() throws MuleException {
      this.scheduler = schedulerService.ioScheduler();
      asyncMessageProcessor.setScheduler(scheduler);
    }

    @Override
    public void stop() throws MuleException {
      if (scheduler != null) {
        scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
        asyncMessageProcessor.setScheduler(null);
      }
    }
  }
}
