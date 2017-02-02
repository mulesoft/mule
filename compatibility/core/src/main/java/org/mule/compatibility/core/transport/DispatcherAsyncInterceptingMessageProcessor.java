/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static java.util.Objects.requireNonNull;
import static javax.resource.spi.work.WorkManager.INDEFINITE;
import static org.mule.runtime.core.config.i18n.CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;

import org.mule.compatibility.core.work.AbstractMuleEventWork;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AsyncWorkListener;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import javax.resource.spi.work.Work;

/**
 * Legacy {@link AbstractInterceptingMessageProcessor} implementation that schedules asynchronous tasks in a {@link WorkManager}.
 *
 * @since 4.0
 */
@Deprecated
public class DispatcherAsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements MessagingExceptionHandlerAware, InternalMessageProcessor {


  private WorkManagerSource workManagerSource;
  private MessagingExceptionHandler messagingExceptionHandler;

  public DispatcherAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource) {
    requireNonNull(workManagerSource);
    this.workManagerSource = workManagerSource;
  }

  @Override
  public final Event process(Event event) throws MuleException {
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

  private Event processNextTimed(Event event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
      }

      return new ProcessingTimeInterceptor(next).process(event);
    }
  }

  protected boolean canProcessAsync(Event event) {
    return !((flowConstruct instanceof ProcessingDescriptor && ((ProcessingDescriptor) flowConstruct).isSynchronous())
        || isTransactionActive());
  }

  private void processNextAsync(Event event) throws MuleException {
    try {
      doProcessNextAsync(new AsyncMessageProcessorWorker(event));
      fireAsyncScheduledNotification(event);
    } catch (Exception e) {
      throw new MessagingException(errorSchedulingMessageProcessorForAsyncInvocation(next), event, e, this);
    }
  }

  private void fireAsyncScheduledNotification(Event event) {
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(flowConstruct, event, next, PROCESS_ASYNC_SCHEDULED));
  }

  @Override
  public final void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
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
          } catch (MessagingException e) {
            exceptionThrown = e;
            throw e;
          } catch (Exception e) {
            exceptionThrown = new MessagingException(response != null ? response : event, e, next);
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

  private void firePipelineNotification(Event event, MessagingException exception) {
    // Async completed notification uses same event instance as async listener
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(flowConstruct, event, next, PROCESS_ASYNC_COMPLETE, exception));
  }

  private boolean isProcessAsync(Event event) {
    return canProcessAsync(event);
  }

  private void doProcessNextAsync(Work work) throws Exception {
    workManagerSource.getWorkManager().scheduleWork(work, INDEFINITE, null, new AsyncWorkListener(next));
  }

}
