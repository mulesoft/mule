/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NamedObject;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.FailedToQueueEventException;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.management.stats.QueueStatistics;
import org.mule.service.Pausable;
import org.mule.service.Resumable;
import org.mule.util.concurrent.WaitableBoolean;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import java.text.MessageFormat;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to
 * schedule asynchronous processing of the next {@link MessageProcessor}.
 */
public class SedaStageInterceptingMessageProcessor extends OptionalAsyncInterceptingMessageProcessor
    implements Work, Lifecycle, Pausable, Resumable
{
    protected static final String QUEUE_NAME_PREFIX = "seda.queue";

    protected QueueProfile queueProfile;
    protected int queueTimeout;
    protected QueueStatistics queueStatistics;
    protected MuleContext muleContext;
    protected String name;
    protected Queue queue;
    private WaitableBoolean running = new WaitableBoolean(false);;
    protected SedaStageLifecycleManager lifecycleManager;

    public SedaStageInterceptingMessageProcessor(String name,
                                                 QueueProfile queueProfile,
                                                 int queueTimeout,
                                                 ThreadingProfile threadingProfile,
                                                 QueueStatistics queueStatistics,
                                                 MuleContext muleContext)
    {
        super(threadingProfile, "seda." + name, muleContext.getConfiguration().getShutdownTimeout());
        this.name = name;
        this.queueProfile = queueProfile;
        this.queueTimeout = queueTimeout;
        this.queueStatistics = queueStatistics;
        this.muleContext = muleContext;
        lifecycleManager = new SedaStageLifecycleManager(name, this);
    }

    @Override
    protected void processNextAsync(MuleEvent event) throws MuleException
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

        MuleEvent event = (MuleEvent) queue.poll(queueTimeout);
        // If the service has been paused why the poll was waiting for an event to
        // arrive on the queue,
        // we put the object back on the queue
        if (event != null && lifecycleManager.isPhaseComplete(Pausable.PHASE_NAME))
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
                processNextTimed(event);
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
                    exceptionListener.handleException(
                        new MessagingException(CoreMessages.eventProcessingFailedFor(getStageDescription()),
                            event, e), event);
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

        running.set(true);
        while (!lifecycleManager.getState().isStopped())
        {
            try
            {
                // Wait if the service is paused
                if (lifecycleManager.isPhaseComplete(Pausable.PHASE_NAME))
                {
                    waitIfPaused();

                    // If service is resumed as part of stopping
                    if (lifecycleManager.getState().isStopping())
                    {
                        if (!isQueuePersistent() && (queueSession != null && getQueueSize() > 0))
                        {
                            // Any messages in a non-persistent queue went paused
                            // service is stopped are lost
                            logger.warn(CoreMessages.stopPausedSedaStageNonPeristentQueueMessageLoss(
                                getQueueSize(), getQueueName()));
                        }
                        break;
                    }
                }

                // If we're doing a draining stop, read all events from the queue
                // before stopping
                if (lifecycleManager.getState().isStopping())
                {
                    if (isQueuePersistent() || queueSession == null || getQueueSize() <= 0)
                    {
                        break;
                    }
                }

                event = (DefaultMuleEvent) dequeue();
            }
            catch (InterruptedException ie)
            {
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
                        CoreMessages.eventProcessingFailedFor(getStageDescription()), event, e));
                }
            }

            if (event != null)
            {
                if (isStatsEnabled())
                {
                    queueStatistics.decQueuedEvent();
                }

                if (logger.isDebugEnabled())
                {
                    logger.debug(MessageFormat.format("{0}: Dequeued event from {1}", getStageDescription(),
                        getQueueName()));
                }
                AbstractMuleEventWork work = new SedaStageWorker(event);
                if (doThreading)
                {
                    try
                    {
                        workManagerSource.getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null,
                            new AsyncWorkListener(next));
                    }
                    catch (Exception e)
                    {
                        // Use the event copy created in SedaStageWorker constructor
                        // because dequeued event may still be owned by a previuos
                        // thread
                        OptimizedRequestContext.unsafeSetEvent(work.getEvent());
                        event.getFlowConstruct().getExceptionListener().handleException(e, work.getEvent());
                    }
                }
                else
                {
                    work.run();
                }
            }
        }
        running.set(false);
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
        if (logger.isDebugEnabled() && lifecycleManager.isPhaseComplete(Pausable.PHASE_NAME))
        {
            logger.debug(getStageDescription() + " is paused. Polling halted until resumed is called");
        }
        while (lifecycleManager.isPhaseComplete(Pausable.PHASE_NAME)
               && !lifecycleManager.getState().isStopping())
        {
            Thread.sleep(50);
        }
    }

    public void release()
    {
        running.set(false);
    }

    public void initialise() throws InitialisationException
    {
        lifecycleManager.fireInitialisePhase(new LifecycleCallback<SedaStageInterceptingMessageProcessor>()
        {
            public void onTransition(String phaseName, SedaStageInterceptingMessageProcessor object)
                throws MuleException
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
                    throw new InitialisationException(
                        MessageFactory.createStaticMessage("Queue not created for " + getStageDescription()),
                        SedaStageInterceptingMessageProcessor.this);
                }
            }
        });
    }

    @Override
    public void start() throws MuleException
    {
        lifecycleManager.fireStartPhase(new LifecycleCallback<SedaStageInterceptingMessageProcessor>()
        {
            public void onTransition(String phaseName, SedaStageInterceptingMessageProcessor object)
                throws MuleException
            {
                if (queue == null)
                {
                    throw new IllegalStateException("Not initialised");
                }
                SedaStageInterceptingMessageProcessor.super.start();
                try
                {
                    workManagerSource.getWorkManager().scheduleWork(
                        SedaStageInterceptingMessageProcessor.this, WorkManager.INDEFINITE, null,
                        new AsyncWorkListener(next));
                }
                catch (WorkException e)
                {
                    throw new LifecycleException(CoreMessages.failedToStart(getStageDescription()), e, this);

                }
            }
        });
    }

    @Override
    public void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase(new LifecycleCallback<SedaStageInterceptingMessageProcessor>()
        {
            public void onTransition(String phaseName, SedaStageInterceptingMessageProcessor object)
                throws MuleException
            {
                try
                {
                    running.whenFalse(null);
                }
                catch (InterruptedException e)
                {
                    // we can ignore this
                }
                SedaStageInterceptingMessageProcessor.super.stop();
            }
        });
    }

    public void dispose()
    {
        lifecycleManager.fireDisposePhase(new LifecycleCallback<SedaStageInterceptingMessageProcessor>()
        {
            public void onTransition(String phaseName, SedaStageInterceptingMessageProcessor object)
                throws MuleException
            {
                queue = null;
            }
        });
    }

    public void pause() throws MuleException
    {
        lifecycleManager.firePausePhase(new EmptyLifecycleCallback<SedaStageInterceptingMessageProcessor>());
    }

    public void resume() throws MuleException
    {
        lifecycleManager.fireResumePhase(new EmptyLifecycleCallback<SedaStageInterceptingMessageProcessor>());
    }

}
