/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * Creates {@link WorkQueueProcessingStrategy} instances. This processing strategy dispatches incoming messages to a work queue
 * which is served by a pool of worker threads from the applications IO {@link Scheduler}. Processing of the flow is carried out
 * synchronously on the worker thread until completion.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueProcessingStrategyFactory implements ProcessingStrategyFactory {

  private int maxThreads;

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(config().withMaxConcurrentTasks(maxThreads)
            .withName(schedulersNamePrefix)),
                                           scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                       MILLISECONDS),
                                           muleContext);
  }

  static class WorkQueueProcessingStrategy extends AbstractSchedulingProcessingStrategy {

    private Supplier<Scheduler> schedulerSupplier;
    private Scheduler scheduler;

    public WorkQueueProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper,
                                       MuleContext muleContext) {
      super(schedulerStopper, muleContext);
      this.schedulerSupplier = schedulerSupplier;
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          .publishOn(fromExecutorService(scheduler))
          .transform(pipelineFunction);
    }

    @Override
    public void start() throws MuleException {
      this.scheduler = schedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (scheduler != null) {
        getSchedulerStopper().accept(scheduler);
      }
    }

  }
}
