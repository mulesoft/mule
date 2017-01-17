/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.rx.Exceptions.UNEXPECTED_EXCEPTION_PREDICATE;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.rx.Exceptions.EventDroppedException;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.session.DefaultMuleSession;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.resource.spi.work.WorkManager;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * This factory's strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single
 * worker thread.
 */
@Deprecated
public class LegacyAsynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  private static final Logger LOGGER = getLogger(LegacyAsynchronousProcessingStrategyFactory.class);

  public static final String SYNCHRONOUS_EVENT_ERROR_MESSAGE = "Unable to process a synchronous event asynchronously";

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new LegacyAsynchronousProcessingStrategy(() -> muleContext.getSchedulerService()
        .ioScheduler(config().withName(schedulersNamePrefix)),
                                                    scheduler -> scheduler
                                                        .stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS),
                                                    muleContext);
  }

  @Deprecated
  static class LegacyAsynchronousProcessingStrategy extends AbstractSchedulingProcessingStrategy {


    private Supplier<Scheduler> schedulerSupplier;
    private Scheduler scheduler;

    public LegacyAsynchronousProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper,
                                                MuleContext muleContext) {
      super(schedulerStopper, muleContext);
      this.schedulerSupplier = schedulerSupplier;
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {

      // Conserve existing 3.x async processing strategy behaviuor:
      // i) The request event is echoed rather than the the result of async processing returned
      // ii) Any exceptions that occur due to async processing are not propagated upwards
      return publisher -> from(publisher)
          .doOnNext(assertCanProcess())
          .doOnNext(fireAsyncScheduledNotification(flowConstruct))
          .doOnNext(request -> just(request)
              .map(event -> Event.builder(event).session(new DefaultMuleSession(event.getSession())).build())
              .publishOn(fromExecutorService(scheduler))
              .transform(pipelineFunction)
              .doOnNext(event -> fireAsyncCompleteNotification(event, flowConstruct, null))
              .doOnError(MessagingException.class, e -> fireAsyncCompleteNotification(e.getEvent(), flowConstruct, e))
              .onErrorResumeWith(MessagingException.class, messagingExceptionHandler)
              .onErrorResumeWith(EventDroppedException.class, mde -> empty())
              .doOnError(UNEXPECTED_EXCEPTION_PREDICATE, exception -> LOGGER.error("Unhandled exception in async processing.",
                                                                                   exception))
              .subscribe());
    }

    protected Consumer<Event> assertCanProcess() {
      return event -> {
        if (isTransactionActive()) {
          throw propagate(new DefaultMuleException(createStaticMessage(SYNCHRONOUS_EVENT_ERROR_MESSAGE)));
        }
      };
    }

    @Override
    public void start() throws MuleException {
      this.scheduler = schedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      getSchedulerStopper().accept(scheduler);
    }

    protected Consumer<Event> fireAsyncScheduledNotification(FlowConstruct flowConstruct) {
      return event -> getMuleContext().getNotificationManager()
          .fireNotification(new AsyncMessageNotification(flowConstruct, event, null, PROCESS_ASYNC_SCHEDULED));
    }

    protected void fireAsyncCompleteNotification(Event event, FlowConstruct flowConstruct, MessagingException exception) {
      getMuleContext().getNotificationManager()
          .fireNotification(new AsyncMessageNotification(flowConstruct, event, null, PROCESS_ASYNC_COMPLETE, exception));
    }

  }

}
