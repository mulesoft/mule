/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
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
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import java.beans.ExceptionListener;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to
 * schedule asynchronous processing of the next {@link MessageProcessor}. The next
 * {@link MessageProcessor} will therefore be executed in a different thread unless
 * the event is synchronous in which case the next {@link MessageProcessor} is
 * invoked directly in the same thread.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements WorkListener
{
    protected WorkManagerSource workManagerSource;
    protected ExceptionListener exceptionListener;
    protected boolean doThreading;

    public AsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource, boolean doThreading, ExceptionListener exceptionListener)
    {
        this.workManagerSource = workManagerSource;
        this.exceptionListener = exceptionListener;
        this.doThreading = doThreading;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else if (event.getEndpoint().isSynchronous()
                 || event.getEndpoint().getTransactionConfig().isTransacted())
        {
            return processNext(event);
        }
        else
        {
            processAsync(event);
            return null;
        }
    }

    protected void processAsync(MuleEvent event) throws MuleException
    {
        try
        {
            Work work = new AsyncMessageProcessorWoker(event);
            if (doThreading)
            {
                workManagerSource.getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, this);
            }
            else
            {
                work.run();
            }
        }
        catch (Exception e)
        {
            new MessagingException(CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation(next),
                event.getMessage(), e);
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

        if (e instanceof Exception)
        {
            exceptionListener.exceptionThrown((Exception) e);
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.errorInvokingMessageProcessorAsynchronously(next), e);
        }
    }

    class AsyncMessageProcessorWoker extends AbstractMuleEventWork
    {
        public AsyncMessageProcessorWoker(MuleEvent event)
        {
            super(event);
        }

        @Override
        protected void doRun()
        {
            try
            {
                processNext(event);
            }
            catch (MuleException e)
            {
                exceptionListener.exceptionThrown(e);
            }
        }
    }

}
