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
import org.mule.api.NameableObject;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.FailedToQueueEventException;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.management.stats.QueueStatistics;
import org.mule.service.Pausable;
import org.mule.service.Resumable;
import org.mule.util.concurrent.WaitableBoolean;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueSession;
import org.mule.work.MuleWorkManager;

import java.text.MessageFormat;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous
 * processing of the next {@link MessageProcessor}.
 */
public class SedaStageInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
    implements Work, Lifecycle, Pausable, Resumable
{
    protected static final String QUEUE_NAME_PREFIX = "seda.queue";

    protected QueueProfile queueProfile;
    protected int queueTimeout;
    protected QueueStatistics queueStatistics;
    protected String queueName;
    protected Queue queue;
    protected QueueConfiguration queueConfiguration;
    private WaitableBoolean running = new WaitableBoolean(false);
    protected SedaStageLifecycleManager lifecycleManager;

    public SedaStageInterceptingMessageProcessor(String threadName,
                                                 String queueName,
                                                 QueueProfile queueProfile,
                                                 int queueTimeout,
                                                 ThreadingProfile threadingProfile,
                                                 QueueStatistics queueStatistics,
                                                 MuleContext muleContext)
    {
        super(threadingProfile, threadName, muleContext.getConfiguration().getShutdownTimeout());
        this.queueName = queueName;
        this.queueProfile = queueProfile;
        this.queueTimeout = queueTimeout;
        this.queueStatistics = queueStatistics;
        this.muleContext = muleContext;
        lifecycleManager = new SedaStageLifecycleManager(queueName, this);
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
            // Events to be processed asynchronously should be copied before they are queued, otherwise
            // concurrent modification of the event could occur.
            enqueue(DefaultMuleEvent.copy(event));
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
        fireAsyncScheduledNotification(event);
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

    /**
     * Roll back the previous dequeue(), i.e., put the event at the front of the queue, not at the back which
     * is what enqueue() does.
     */
    protected void rollbackDequeue(MuleEvent event)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("{1}: Putting event back on queue {2}", queue.getName(),
                getStageDescription(), event));
        }
        try
        {
            queue.untake(event);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    /**
     * While the service isn't stopped this runs a continuous loop checking for new events in the queue.
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
                            // Any messages in a non-persistent queue when paused
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

                event = (DefaultMuleEvent)dequeue();
            }
            catch (InterruptedException ie)
            {
                break;
            }
            catch (Exception e)
            {
                muleContext.getExceptionListener().handleException(e);
            }

            if (event != null)
            {
                final MuleEvent eventToProcess = event;
                TransactionalErrorHandlingExecutionTemplate executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(muleContext, event.getFlowConstruct().getExceptionListener());
                ExecutionCallback<MuleEvent> processingCallback = new ExecutionCallback<MuleEvent>()
                {

                    @Override
                    public MuleEvent process() throws Exception
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
                        AsyncMessageProcessorWorker work = new AsyncMessageProcessorWorker(eventToProcess);
                        try
                        {
                            // TODO Remove this thread handoff to ensure Zero Message Loss
                            workManagerSource.getWorkManager().scheduleWork(work, WorkManager.INDEFINITE,
                                null, new AsyncWorkListener(next));
                        }
                        catch (Exception e)
                        {
                            // because dequeued event may still be owned by a previuos
                            // thread we need to use the copy created in AsyncMessageProcessorWorker
                            // constructor.
                            OptimizedRequestContext.unsafeSetEvent(work.getEvent());
                            throw new MessagingException(work.getEvent(), e, SedaStageInterceptingMessageProcessor.this);
                        }
                        return null;
                    }
                };

                try
                {
                    executionTemplate.execute(processingCallback);
                }
                catch (MessagingException e)
                {
                    //Already handled by processing template
                }
                catch (Exception e)
                {
                    muleContext.getExceptionListener().handleException(e);
                }
            }
        }
        running.set(false);
    }

    /** Are the events in the SEDA queue persistent? */
    protected boolean isQueuePersistent()
    {
        return queueConfiguration == null ? false : queueConfiguration.isPersistent();
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
        if (queueName != null)
        {
            return queueName;
        }
        else if (next instanceof NameableObject)
        {
            return ((NameableObject)next).getName();
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
                queueConfiguration = queueProfile.configureQueue(getMuleContext(), getQueueName(),
                    muleContext.getQueueManager());
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
