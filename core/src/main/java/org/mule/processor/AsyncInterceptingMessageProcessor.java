/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.AsyncMessageNotification;
import org.mule.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to
 * schedule asynchronous processing of the next {@link MessageProcessor}. The next
 * {@link MessageProcessor} is therefore be executed in a different thread regardless
 * of the exchange-pattern configured on the inbound endpoint. If a transaction is
 * present then an exception is thrown.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements Startable, Stoppable, MessagingExceptionHandlerAware
{

    public static final String SYNCHRONOUS_EVENT_ERROR_MESSAGE = "Unable to process a synchronous event asynchronously";

    protected WorkManagerSource workManagerSource;
    protected boolean doThreading = true;
    protected WorkManager workManager;

    private MessagingExceptionHandler messagingExceptionHandler;

    public AsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource)
    {
        this.workManagerSource = workManagerSource;
    }

    public AsyncInterceptingMessageProcessor(ThreadingProfile threadingProfile,
                                             String name,
                                             int shutdownTimeout)
    {
        this.doThreading = threadingProfile.isDoThreading();
        workManager = threadingProfile.createWorkManager(name, shutdownTimeout);
        workManagerSource = new WorkManagerSource()
        {
            public WorkManager getWorkManager() throws MuleException
            {
                return workManager;
            }
        };
    }

    public void start() throws MuleException
    {
        if (workManager != null)
        {
            workManager.start();
        }
    }

    public void stop() throws MuleException
    {
        if (workManager != null)
        {
            workManager.dispose();
        }
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else if (isProcessAsync(event))
        {
            processNextAsync(event);
            return VoidMuleEvent.getInstance();
        }
        else
        {
            MuleEvent response = processNext(event);
            return response;
        }
    }

    protected MuleEvent processNextTimed(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
            }

            MuleEvent response;
            if (event.getFlowConstruct() != null)
            {
                response = new ProcessingTimeInterceptor(next, event.getFlowConstruct()).process(event);
            }
            else
            {
                response = processNext(event);
            }
            return response;
        }
    }

    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {
        if (event.isSynchronous() || event.isTransacted())
        {
            throw new MessagingException(
                CoreMessages.createStaticMessage(SYNCHRONOUS_EVENT_ERROR_MESSAGE),
                event, this);
        }
        return doThreading && !event.isSynchronous();
    }

    protected void processNextAsync(MuleEvent event) throws MuleException
    {
        try
        {
            workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWorker(DefaultMuleEvent.copy(event)),
                WorkManager.INDEFINITE, null, new AsyncWorkListener(next));
            fireAsyncScheduledNotification(event);
        }
        catch (Exception e)
        {
            new MessagingException(CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation(next),
                event, e, this);
        }
    }

    protected void fireAsyncScheduledNotification(MuleEvent event)
    {
        if (event.getFlowConstruct() instanceof MessageProcessorPathResolver)
        {
            muleContext.getNotificationManager().fireNotification(
                    new AsyncMessageNotification(event.getFlowConstruct(), event, next,
                                                 AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED));
        }

    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        if (this.messagingExceptionHandler == null)
        {
            this.messagingExceptionHandler = messagingExceptionHandler;
        }
    }

    class AsyncMessageProcessorWorker extends AbstractMuleEventWork
    {
        public AsyncMessageProcessorWorker(MuleEvent event)
        {
            super(event);
        }

        @Override
        protected void doRun()
        {
            MessagingExceptionHandler exceptionHandler = messagingExceptionHandler;
            ExecutionTemplate<MuleEvent> executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(
                    muleContext, new MuleTransactionConfig(), exceptionHandler);

            try
            {
                executionTemplate.execute(new ExecutionCallback<MuleEvent>()
                {
                    @Override
                    public MuleEvent process() throws Exception
                    {
                        MessagingException exceptionThrown = null;
                        try
                        {
                            processNextTimed(event);
                        }
                        catch (MessagingException e)
                        {
                            exceptionThrown = e;
                            throw e;
                        }
                        catch (Exception e)
                        {
                            exceptionThrown = new MessagingException(event, e, next);
                            throw exceptionThrown;
                        }
                        finally
                        {
                            firePipelineNotification(event, exceptionThrown);
                        }
                        return VoidMuleEvent.getInstance();
                    }
                });
            }
            catch (MessagingException e)
            {
                // Already handled by TransactionTemplate
            }
            catch (Exception e)
            {
                muleContext.getExceptionListener().handleException(e);
            }
        }
    }

    protected void firePipelineNotification(MuleEvent event, MessagingException exception)
    {
        if (event.getFlowConstruct() instanceof MessageProcessorPathResolver)
        {
            muleContext.getNotificationManager().fireNotification(
                new AsyncMessageNotification(event.getFlowConstruct(), event,
                    next, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE, exception));
        }
    }

}
