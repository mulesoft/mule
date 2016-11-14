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
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.exception.MessagingException;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.resource.spi.work.WorkManager;

import org.reactivestreams.Publisher;

/**
 * This factory's strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single
 * worker thread.
 */
public class AsynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new AsynchronousProcessingStrategy(() -> {
      try {
        return muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }, scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS), muleContext);
  }

  static class AsynchronousProcessingStrategy implements ProcessingStrategy, Startable, Stoppable {

    private Supplier<Scheduler> schedulerSupplier;
    private Consumer<Scheduler> schedulerStopper;
    private Scheduler scheduler;
    private MuleContext muleContext;

    public AsynchronousProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper,
                                          MuleContext muleContext) {
      this.schedulerSupplier = schedulerSupplier;
      this.schedulerStopper = schedulerStopper;
      this.muleContext = muleContext;
    }

    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction) {

      return publisher -> from(publisher)
          .doOnNext(assertCanProcessAsync())
          .doOnNext(fireAsyncScheduledNotification(flowConstruct))
          .publishOn(fromExecutorService(scheduler))
          .transform(pipelineFunction)
          .doOnNext(request -> fireAsyncCompleteNotification(request, flowConstruct, null))
          .doOnError(MessagingException.class,
                     msgException -> fireAsyncCompleteNotification(msgException.getEvent(), flowConstruct, msgException));
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

    private Consumer<Event> assertCanProcessAsync() {
      return event -> {
        if (isTransactionActive()) {
          throw propagate(new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE)));
        }
      };
    }

    protected Consumer<Event> fireAsyncScheduledNotification(FlowConstruct flowConstruct) {
      return event -> muleContext.getNotificationManager()
          .fireNotification(new AsyncMessageNotification(flowConstruct, event, null, PROCESS_ASYNC_SCHEDULED));
    }

    protected void fireAsyncCompleteNotification(Event event, FlowConstruct flowConstruct, MessagingException exception) {
      muleContext.getNotificationManager()
          .fireNotification(new AsyncMessageNotification(flowConstruct, event, null, PROCESS_ASYNC_COMPLETE, exception));
    }

    protected Scheduler getScheduler() {
      return this.scheduler;
    }
  }
}
