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
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import java.util.Collections;
import java.util.List;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous
 * processing of MessageProcessor delegate configured the next {@link MessageProcessor}. The next {@link MessageProcessor} is therefore be executed
 * in a different thread regardless of the exchange-pattern configured on the inbound endpoint. If a
 * transaction is present then an exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements MessageProcessor, Startable, Stoppable, WorkListener
{
    protected Log logger = LogFactory.getLog(getClass());

    protected WorkManagerSource workManagerSource;
    protected boolean doThreading = true;
    protected WorkManager workManager;
    protected MessageProcessor delegate;

    public AsyncDelegateMessageProcessor(ThreadingProfile threadingProfile, String name, int shutdownTimeout)
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
        super.start();
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
        // There is no need to copy the event here because it is copied in
        // org.mule.work.AbstractMuleEventWork.run()
        if (delegate != null)
        {
            try
            {
                workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWork(event),
                    WorkManager.INDEFINITE, null, this);
            }
            catch (Exception e)
            {
                new MessagingException(
                    CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation(delegate), event, e);
            }
        }
        return event;
    }

    public void setDelegate(MessageProcessor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return Collections.singletonList(delegate);
    }

    class AsyncMessageProcessorWork extends AbstractMuleEventWork
    {
        public AsyncMessageProcessorWork(MuleEvent event)
        {
            super(event);
        }

        @Override
        protected void doRun()
        {
            try
            {
                delegate.process(event);
            }
            catch (Exception e)
            {
                MessagingExceptionHandler exceptionListener = event.getFlowConstruct().getExceptionListener();
                if (e instanceof MessagingException)
                {
                    exceptionListener.handleException(e, event);
                }
                else
                {
                    exceptionListener.handleException(
                        new MessagingException(CoreMessages.eventProcessingFailedFor(this.toString()), event,
                            e), event);
                }
            }
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

        if (e instanceof MessagingException)
        {
            MuleEvent muleEvent = ((MessagingException)e).getEvent();
            muleEvent.getFlowConstruct().getExceptionListener().handleException((Exception)e, muleEvent);
        }
        else if (e instanceof Exception)
        {
            muleContext.getExceptionListener().handleException((Exception)e);
        }
        else
        {
            logger.error("Work caused exception " + e + "on '" + type + "'. Work being executed was: "
                         + event.getWork().toString());
        }
    }

}
