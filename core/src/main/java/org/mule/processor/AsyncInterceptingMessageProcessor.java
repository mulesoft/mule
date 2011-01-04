/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to
 * schedule asynchronous processing of the next {@link MessageProcessor}. The next
 * {@link MessageProcessor} is therefore be executed in a different thread regardless
 * of the exchange-pattern configured on the inbound endpoint. If a transaction is
 * present then an exception is thrown.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements WorkListener, Startable, Stoppable
{
    protected WorkManagerSource workManagerSource;
    protected boolean doThreading = true;
    protected WorkManager workManager;

    public AsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource)
    {
        this.workManagerSource = workManagerSource;
    }

    @Deprecated
    public AsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource, boolean doThreading)
    {
        this.workManagerSource = workManagerSource;
        this.doThreading = doThreading;
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
            return null;
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
                response = next.process(event);
            }
            return response;
        }
    }

    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {
        // We do not support transactions and async
        if (event.getEndpoint().getTransactionConfig().isTransacted())
        {
            throw new MessagingException(CoreMessages.asyncDoesNotSupportTransactions(), event);
        }
        return doThreading;
    }

    protected void processNextAsync(MuleEvent event) throws MuleException
    {
        try
        {
            workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWorker(event),
                WorkManager.INDEFINITE, null, this);
        }
        catch (Exception e)
        {
            new MessagingException(CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation(next),
                event, e);
        }
    }

    public void workAccepted(WorkEvent event)
    {
        this.handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event)
    {
        this.handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event)
    {
        this.handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event)
    {
        this.handleWorkException(event, "workCompleted");
    }

    protected void handleWorkException(WorkEvent event, String type)
    {
        if (event == null)
        {
            return;
        }

        Throwable e = event.getException();

        if (e == null)
        {
            return;
        }

        if (e.getCause() != null)
        {
            e = e.getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                     + event.getWork().toString());
        throw new MuleRuntimeException(CoreMessages.errorInvokingMessageProcessorAsynchronously(next), e);
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
            try
            {
                processNextTimed(event);
            }
            catch (MuleException e)
            {
                event.getFlowConstruct().getExceptionListener().handleException(e, event);
            }
        }
    }

}
