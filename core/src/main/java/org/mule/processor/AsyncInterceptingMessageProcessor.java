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
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.process.ProcessingCallback;
import org.mule.process.ProcessingTemplate;
import org.mule.process.TransactionalErrorHandlingProcessingTemplate;
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
    implements Startable, Stoppable
{
    protected WorkManagerSource workManagerSource;
    protected boolean doThreading = true;
    protected WorkManager workManager;

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
                CoreMessages.createStaticMessage("Unable to process a synchonrous event asyncronously"),
                event);
        }
        return doThreading && !event.isSynchronous();
    }

    protected void processNextAsync(MuleEvent event) throws MuleException
    {
        try
        {
            workManagerSource.getWorkManager().scheduleWork(new AsyncMessageProcessorWorker(event),
                WorkManager.INDEFINITE, null, new AsyncWorkListener(next));
        }
        catch (Exception e)
        {
            new MessagingException(CoreMessages.errorSchedulingMessageProcessorForAsyncInvocation(next),
                event, e);
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
            ProcessingTemplate<MuleEvent> processingTemplate = new TransactionalErrorHandlingProcessingTemplate(muleContext,new MuleTransactionConfig(),event.getFlowConstruct().getExceptionListener());
            try
            {
                processingTemplate.execute(new ProcessingCallback<MuleEvent>() {
                    @Override
                    public MuleEvent process() throws Exception
                    {
                        try
                        {
                            processNextTimed(event);
                        }
                        catch (MessagingException e)
                        {
                            throw e;
                        }
                        catch (Exception e)
                        {
                            throw new MessagingException(event,e);
                        }
                        return null;
                    }
                });
            }
            catch (MessagingException e)
            {
                //Already handled by TransactionTemplate
            }
            catch (Exception e)
            {
                muleContext.getExceptionListener().handleException(e);
            }
        }
    }

}
