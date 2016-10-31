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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create() {
    return new NonBlockingProcessingStrategy();
  }

  public static class NonBlockingProcessingStrategy implements ProcessingStrategy, Stoppable {

    private SchedulerService schedulerService;
    private Scheduler scheduler;
    private MuleContext muleContext;

    @Override
    public void configureProcessors(List<Processor> processors, SchedulerService schedulerService,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      this.schedulerService = schedulerService;
      this.muleContext = muleContext;
      for (Processor processor : processors) {
        chainBuilder.chain(processor);
      }
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor processor,
                                                                    Function<Publisher<Event>, Publisher<Event>> publisherFunction) {
      this.scheduler = schedulerService.cpuLightScheduler();
      if (processor instanceof NonBlockingMessageProcessor) {
        return publisher -> from(publisher).transform(publisherFunction).publishOn(fromExecutorService(scheduler));
      } else {
        return publisher -> from(publisher).transform(publisherFunction);
      }
    }

    @Override
    public void stop() throws MuleException {
      scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
    }
  }
}
