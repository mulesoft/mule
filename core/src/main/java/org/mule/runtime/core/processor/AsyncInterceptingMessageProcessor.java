/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.config.i18n.CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.work.AbstractMuleEventWork;
import org.mule.runtime.core.work.MuleWorkManager;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous processing of the next
 * {@link MessageProcessor}. The next {@link MessageProcessor} is therefore be executed in a different thread regardless of the
 * exchange-pattern configured on the inbound endpoint. If a transaction is present then an exception is thrown.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements Startable, Stoppable, MessagingExceptionHandlerAware, NonBlockingSupported {

  public static final String SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE =
      "Unable to process a synchronous or non-blocking event asynchronously";

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
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (next == null) {
      return event;
    } else if (isProcessAsync(event)) {
      processNextAsync(event);
      return VoidMuleEvent.getInstance();
    } else {
      MuleEvent response = processNext(event);
      return response;
    }
  }

  protected MuleEvent processNextTimed(MuleEvent event) throws MuleException {
    if (next == null) {
      return event;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
      }

      return new ProcessingTimeInterceptor(next).process(event);
    }
  }

  protected boolean isProcessAsync(MuleEvent event) throws MessagingException {
    if (!canProcessAsync(event)) {
      throw new MessagingException(CoreMessages.createStaticMessage(SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE), event, this);
    }
    return doThreading && canProcessAsync(event);
  }

  protected boolean canProcessAsync(MuleEvent event) throws MessagingException {
    return !(event.isSynchronous() || event.isTransacted() || event.getReplyToHandler() instanceof NonBlockingReplyToHandler);
  }

  protected void processNextAsync(MuleEvent event) throws MuleException {
    try {
      workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWorker(event), WorkManager.INDEFINITE, null,
                                                      new AsyncWorkListener(next));
      fireAsyncScheduledNotification(event);
    } catch (Exception e) {
      new MessagingException(errorSchedulingMessageProcessorForAsyncInvocation(next), event, e, this);
    }
  }

  protected void fireAsyncScheduledNotification(MuleEvent event) {
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

    public AsyncMessageProcessorWorker(MuleEvent event) {
      super(event, true);
    }

    public AsyncMessageProcessorWorker(MuleEvent event, boolean copy) {
      super(event, copy);
    }

    @Override
    protected void doRun() {
      MessagingExceptionHandler exceptionHandler = messagingExceptionHandler;
      ExecutionTemplate<MuleEvent> executionTemplate =
          createMainExecutionTemplate(muleContext, flowConstruct, new MuleTransactionConfig(), exceptionHandler);

      try {
        executionTemplate.execute(() -> {
          MessagingException exceptionThrown = null;
          try {
            processNextTimed(event);
          } catch (MessagingException e1) {
            exceptionThrown = e1;
            throw e1;
          } catch (Exception e2) {
            exceptionThrown = new MessagingException(event, e2, next);
            throw exceptionThrown;
          } finally {
            firePipelineNotification(event, exceptionThrown);
          }
          return VoidMuleEvent.getInstance();
        });
      } catch (MessagingException e) {
        // Already handled by TransactionTemplate
      } catch (Exception e) {
        muleContext.getExceptionListener().handleException(e);
      }
    }
  }

  protected void firePipelineNotification(MuleEvent event, MessagingException exception) {
    // Async completed notification uses same event instance as async listener
    muleContext.getNotificationManager()
        .fireNotification(new AsyncMessageNotification(flowConstruct, event, next, PROCESS_ASYNC_COMPLETE, exception));
  }

}
