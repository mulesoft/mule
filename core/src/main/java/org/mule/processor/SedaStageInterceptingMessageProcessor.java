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

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NamedObject;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.FailedToQueueEventException;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.QueueStatistics;
import org.mule.service.Pausable;
import org.mule.util.concurrent.WaitableBoolean;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import java.text.MessageFormat;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to
 * schedule asynchronous processing of the next {@link MessageProcessor}. The next
 * {@link MessageProcessor} will therefore be executed in a different thread unless
 * the event is synchronous in which case the next {@link MessageProcessor} is
 * invoked directly in the same thread.
 */
public class SedaStageInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
    implements WorkListener, Work, Lifecycle
{
    protected static final String QUEUE_NAME_PREFIX = "seda.queue";

    protected QueueProfile queueProfile;
    protected int queueTimeout;
    protected LifecycleState lifecycleState;
    protected QueueStatistics queueStatistics;
    protected MuleContext muleContext;
    protected String name;

    protected Queue queue;
    private WaitableBoolean queueDraining = new WaitableBoolean(false);

    public SedaStageInterceptingMessageProcessor(String name,
                                                 QueueProfile queueProfile,
                                                 int queueTimeout,
                                                 WorkManagerSource workManagerSource,
                                                 boolean doThreading,
                                                 LifecycleState lifecycleState,
                                                 QueueStatistics queueStatistics,
                                                 MuleContext muleContext)
    {
        super(workManagerSource, doThreading);
        this.name = name;
        this.queueProfile = queueProfile;
        this.queueTimeout = queueTimeout;
        this.lifecycleState = lifecycleState;
        this.queueStatistics = queueStatistics;
        this.muleContext = muleContext;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else if (event.getEndpoint().getExchangePattern().hasResponse() || 
                  event.getEndpoint().getTransactionConfig().isTransacted())
        {
            return processNext(event);
        }
        else
        {
            processAsync(event);
            return null;
        }
    }

    @Override
    protected void processAsync(MuleEvent event) throws MuleException
    {
        try
        {
            if (isStatsEnabled())
            {
                queueStatistics.incQueuedEvent();
            }
            enqueue(event);
        }
        catch (Exception e)
        {
            throw new FailedToQueueEventException(
                CoreMessages.interruptedQueuingEventFor(getStageDescription()), event, e);
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("MuleEvent added to queue for: " + getStageDescription());
        }
    }

    protected boolean isStatsEnabled()
    {
        return queueStatistics != null && queueStatistics.isEnabled();
    }

    protected void enqueue(MuleEvent event) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("{1}: Putting event on queue {2}", queue.getName(),
                getStageDescription(), event));
        }
        queue.put(event);
    }

    protected MuleEvent dequeue() throws Exception
    {
        if (queue == null)
        {
            return null;
        }
        if (logger.isTraceEnabled())
        {
            logger.trace(MessageFormat.format("{0}: Polling queue {1}, timeout = {2}", getStageName(),
                getStageDescription(), queueTimeout));
        }

        MuleEvent event = (MuleEvent)queue.poll(queueTimeout);
        //If the service has been paused why the poll was waiting for an event to arrive on the queue,
        //we put the object back on the queue
        if(event!=null && lifecycleState.isPhaseComplete(Pausable.PHASE_NAME))
        {
            queue.untake(event);
            return null;
        }
        return event;
    }

    private class SedaStageWorker extends AbstractMuleEventWork
    {
        public SedaStageWorker(MuleEvent event)
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
            catch (Exception e)
            {
                event.getSession().setValid(false);
                MessagingExceptionHandler exceptionListener = event.getFlowConstruct().getExceptionListener();
                if (e instanceof MessagingException)
                {
                    exceptionListener.handleException(e, event);
                }
                else
                {
                    exceptionListener.handleException(new MessagingException(
                        CoreMessages.eventProcessingFailedFor(getStageDescription()), event, e), event);
                }
            }
        }
    }

    /**
     * While the service isn't stopped this runs a continuous loop checking for new
     * events in the queue.
     */
    public void run()
    {
        DefaultMuleEvent event = null; 
        QueueSession queueSession = muleContext.getQueueManager().getQueueSession();

        while (!lifecycleState.isStopped())
        {
            try
            {
                // Wait if the service is paused
                if (lifecycleState.isPhaseComplete(Pausable.PHASE_NAME))
                {
                    waitIfPaused();

                    // If service is resumed as part of stopping
                    if (lifecycleState.isStopping())
                    {
                        queueDraining.set(true);
                        if (!isQueuePersistent() && (queueSession != null && getQueueSize() > 0))
                        {
                            // Any messages in a non-persistent queue went paused
                            // service is stopped are lost
                            logger.warn(CoreMessages.stopPausedSedaStageNonPeristentQueueMessageLoss(
                                getQueueSize(), getQueueName()));
                        }
                        queueDraining.set(false);
                        break;
                    }
                }

                // If we're doing a draining stop, read all events from the queue
                // before stopping
                if (lifecycleState.isStopping())
                {
                    if (isQueuePersistent() || queueSession == null || getQueueSize() <= 0)
                    {
                        queueDraining.set(false);
                        break;
                    }
                }

                event = (DefaultMuleEvent) dequeue();
            }
            catch (InterruptedException ie)
            {
                queueDraining.set(false);
                break;
            }
            catch (Exception e)
            {
                SystemExceptionHandler exceptionListener = muleContext.getExceptionListener();
                if (e instanceof MuleException)
                {
                    exceptionListener.handleException(e);
                }
                else
                {
                    exceptionListener.handleException(new MessagingException(
                        CoreMessages.eventProcessingFailedFor(getStageDescription()),
                        event, e));
                }
            }

            if (event != null)
            {
                try
                {
                    if (isStatsEnabled())
                    {
                        queueStatistics.decQueuedEvent();
                    }

                    if (logger.isDebugEnabled())
                    {
                        logger.debug(MessageFormat.format("{0}: Dequeued event from {1}",
                            getStageDescription(), getQueueName()));
                    }
                    Work work = new SedaStageWorker(event);
                    if (doThreading)
                    {
                        workManagerSource.getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null,
                            this);
                    }
                    else
                    {
                        work.run();
                    }
                }
                catch (Exception e)
                {
                    event.getFlowConstruct().getExceptionListener().handleException(e, event);
                }
            }
        }
    }

    /** Are the events in the SEDA queue persistent? */
    protected boolean isQueuePersistent()
    {
        return queueProfile.isPersistent();
    }

    public int getQueueSize()
    {
        return queue.size();
    }

    protected String getQueueName()
    {
        return String.format("%s(%s)", QUEUE_NAME_PREFIX, getStageName());
    }

    protected String getStageName()
    {
        if (name != null)
        {
            return name;
        }
        else if (next instanceof NamedObject)
        {
            return ((NamedObject) next).getName();
        }
        else
        {
            return String.format("%s.%s", next.getClass().getName(), next.hashCode());
        }
    }

    protected String getStageDescription()
    {
        return "SEDA Stage " + getStageName();
    }

    protected void waitIfPaused() throws InterruptedException
    {
        if (logger.isDebugEnabled() && lifecycleState.isPhaseComplete(Pausable.PHASE_NAME))
        {
            logger.debug(getStageDescription() + " is paused. Polling halted until resumed is called");
        }
        while (lifecycleState.isPhaseComplete(Pausable.PHASE_NAME) && !lifecycleState.isStopping())
        {
            Thread.sleep(500);
        }
    }

    public void release()
    {
        queueDraining.set(false);
    }

    public void initialise() throws InitialisationException
    {
        if (next == null)
        {
            throw new IllegalStateException(
                "Next message processor cannot be null with this InterceptingMessageProcessor");
        }
        // Setup event Queue
        queueProfile.configureQueue(getQueueName(), muleContext.getQueueManager());
        queue = muleContext.getQueueManager().getQueueSession().getQueue(getQueueName());
        if (queue == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Queue not created for "
                                                                                 + getStageDescription()),
                this);
        }
    }

    public void start() throws MuleException
    {
        if (queue == null)
        {
            throw new IllegalStateException("Not initialised");
        }
        try
        {
            workManagerSource.getWorkManager().scheduleWork(this, WorkManager.INDEFINITE, null, this);
        }
        catch (WorkException e)
        {
            throw new LifecycleException(CoreMessages.failedToStart(getStageDescription()), e, this);

        }
    }

    public void stop() throws MuleException
    {
        if (queue != null && queue.size() > 0)
        {
            try
            {
                queueDraining.whenFalse(null);
            }
            catch (InterruptedException e)
            {
                // we can ignore this
            }
        }
    }

    public void dispose()
    {
        queue = null;
    }

}
