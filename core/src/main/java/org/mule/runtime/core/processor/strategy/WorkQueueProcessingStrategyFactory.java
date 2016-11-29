/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.Predicate;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.resource.spi.work.WorkManager;

import org.reactivestreams.Publisher;
import reactor.core.scheduler.Schedulers;

/**
 * Creates {@link WorkQueueProcessingStrategy} instances. This processing strategy dipatches incoming messages to a work queue
 * which is served by a pool of worker threads from the applications IO {@link Scheduler}. Processing of the flow is carried out
 * synchronously on the worker thread until completion.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";


  // TODO MULE-11062 Need to be able to configure maxiumum number of workers
  private int maxThreads;

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new WorkQueueProcessingStrategy(() -> muleContext.getSchedulerService().ioScheduler(),
                                           maxThreads,
                                           scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                       MILLISECONDS),
                                           muleContext);
  }

  static class WorkQueueProcessingStrategy extends AbstractSchedulingProcessingStrategy {

    private Supplier<Scheduler> schedulerSupplier;
    private Scheduler scheduler;

    public WorkQueueProcessingStrategy(Supplier<Scheduler> schedulerSupplier, int maxThreads,
                                       Consumer<Scheduler> schedulerStopper,
                                       MuleContext muleContext) {
      super(schedulerStopper, muleContext);
      this.schedulerSupplier = schedulerSupplier;
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          .doOnNext(assertCanProcessAsync())
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

    private Consumer<Event> assertCanProcessAsync() {
      return event -> {
        if (isTransactionActive()) {
          throw propagate(new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE)));
        }
      };
    }

    @Override
    protected Predicate<Scheduler> scheduleOverridePredicate() {
      return scheduler -> false;
    }
  }
}
