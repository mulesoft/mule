/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.config.i18n.CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutor;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.work.AbstractMuleEventWork;
import org.mule.runtime.core.work.MuleWorkManager;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Processes {@link Event}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous processing of the next
 * {@link Processor}. The next {@link Processor} is therefore be executed in a different thread regardless of the exchange-pattern
 * configured on the inbound endpoint. If a transaction is present then an exception is thrown.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements Startable, Stoppable, MessagingExceptionHandlerAware, InternalMessageProcessor {

  public static final String SYNCHRONOUS_EVENT_ERROR_MESSAGE = "Unable to process a synchronous event asynchronously";

  protected WorkManagerSource workManagerSource;
  protected boolean doThreading = true;
  protected long threadTimeout;
  protected WorkManager workManager;

  private MessagingExceptionHandler messagingExceptionHandler;

  public AsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource) {
    this.workManagerSource = workManagerSource;
  }

  public AsyncInterceptingMessageProcessor(ThreadingProfile threadingProfile, String name, int shutdownTimeout) {
    this.doThreading = threadingProfile.isDoThreading();
    this.threadTimeout = threadingProfile.getThreadWaitTimeout();
    workManager = threadingProfile.createWorkManager(name, shutdownTimeout);
    workManagerSource = () -> workManager;
  }

  @Override
  public void start() throws MuleException {
    if (workManager != null && !workManager.isStarted()) {
      workManager.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (workManager != null) {
      workManager.dispose();
    }
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (next == null) {
      return event;
    } else if (isProcessAsync(event)) {
      processNextAsync(event);
      return event;
    } else {
      Event response = processNext(event);
      return response;
    }
  }

  protected Event processNextTimed(Event event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
      }

      return new ProcessingTimeInterceptor(next).process(event);
    }
  }

  protected boolean isProcessAsync(Event event) throws MuleException {
    if (!canProcessAsync(event)) {
      throw new DefaultMuleException(createStaticMessage(SYNCHRONOUS_EVENT_ERROR_MESSAGE));
    }
    return doThreading && canProcessAsync(event);
  }

  protected boolean canProcessAsync(Event event) {
    return !(event.isSynchronous() || event.isTransacted());
  }

  protected void processNextAsync(Event event) throws MuleException {
    try {
      workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWorker(event), WorkManager.INDEFINITE, null,
                                                      new AsyncWorkListener(next));
      fireAsyncScheduledNotification(event);
    } catch (Exception e) {
      new MessagingException(errorSchedulingMessageProcessorForAsyncInvocation(next), event, e, this);
    }
  }

  protected void fireAsyncScheduledNotification(Event event) {
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(flowConstruct, event, next, PROCESS_ASYNC_SCHEDULED));
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    if (this.messagingExceptionHandler == null) {
      this.messagingExceptionHandler = messagingExceptionHandler;
    }
  }

  class AsyncMessageProcessorWorker extends AbstractMuleEventWork {

    public AsyncMessageProcessorWorker(Event event) {
      super(event, true);
    }

    @Override
    protected void doRun() {
      MessagingExceptionHandler exceptionHandler = messagingExceptionHandler;
      ExecutionTemplate<Event> executionTemplate =
          createMainExecutionTemplate(muleContext, flowConstruct, new MuleTransactionConfig(), exceptionHandler);

      try {
        executionTemplate.execute(() -> {
          MessagingException exceptionThrown = null;
          Event response = null;
          try {
            response = processNextTimed(event);
          } catch (MessagingException e1) {
            exceptionThrown = e1;
            throw e1;
          } catch (Exception e2) {
            exceptionThrown = new MessagingException(response != null ? response : event, e2, next);
            throw exceptionThrown;
          } finally {
            firePipelineNotification(response != null ? response : event, exceptionThrown);
          }
          return event;
        });
      } catch (MessagingException e) {
        // Already handled by TransactionTemplate
      } catch (Exception e) {
        muleContext.getExceptionListener().handleException(e);
      }
    }
  }

  protected void firePipelineNotification(Event event, MessagingException exception) {
    // Async completed notification uses same event instance as async listener
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(flowConstruct, event, next, PROCESS_ASYNC_COMPLETE, exception));
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return Flux.from(publisher)
        .concatMap(request -> {
          try {
            if (isProcessAsync(request)) {
              just(request).doOnNext(event1 -> fireAsyncScheduledNotification(event1))
                  .map(event -> Event.builder(event).session(new DefaultMuleSession(event.getSession())).build())
                  .transform(stream -> applyNext(stream)).subscribeOn(fromExecutor(workManagerSource.getWorkManager()))
                  .doOnNext(event -> firePipelineNotification(event, null))
                  .doOnError(MessagingException.class, me -> firePipelineNotification(me.getEvent(), me))
                  .onErrorResumeWith(MessagingException.class, flowConstruct.getExceptionListener()).subscribe();
              return just(request);
            } else {
              return just(request).transform(stream -> applyNext(stream));
            }
          } catch (MuleException e) {
            throw propagate(e);
          }
        });
  }

}
