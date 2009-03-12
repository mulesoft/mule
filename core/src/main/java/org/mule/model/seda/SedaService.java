/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.seda;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.FailedToQueueEventException;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.ServiceException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.ServiceStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.service.AbstractService;
import org.mule.transport.NullPayload;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.text.MessageFormat;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.lang.BooleanUtils;

/**
 * A Seda service runs inside a Seda Model and is responsible for managing a Seda
 * Queue and thread pool for a Mule sevice service. In Seda terms this is
 * equivilent to a stage.
 */
public class SedaService extends AbstractService implements Work, WorkListener
{
    /**
     * Serial version/
     */
    private static final long serialVersionUID = 7711976708670893015L;
    
    private static final String QUEUE_NAME_SUFFIX = ".service";

    protected WorkManager workManager;

    /**
     * The time out used for taking from the Seda Queue.
     */
    protected Integer queueTimeout;

    /**
     * The threading profile to use for this service. If this is not set a default
     * will be provided by the server
     */
    protected ThreadingProfile threadingProfile;

    /**
     * The queue profile to use for this service. If this is not set a default
     * will be provided by the server
     */
    protected QueueProfile queueProfile;
    
    protected Queue queue;

    /**
     * A guarding object for queue statistics enqueue/dequeueing.
     */
    private final Object queueStatsGuard = new Object();

    /** For Spring only */
    public SedaService()
    {
        super();
    }
    
    /**
     * Initialise the service. The service will first create a Mule component from the
     * Service and then initialise a pool based on the attributes in the
     * Service.
     * 
     * @throws org.mule.api.lifecycle.InitialisationException if the service fails
     *             to initialise
     */
    protected synchronized void doInitialise() throws InitialisationException
    {
        if (threadingProfile == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            threadingProfile = muleContext.getDefaultServiceThreadingProfile();
        }
        // Create thread pool
        // (Add one to maximum number of active threads to account for the service
        // work item that is running continuously and polling the SEDA queue.)
        ChainedThreadingProfile threadingProfile = new ChainedThreadingProfile(this.threadingProfile);
        threadingProfile.setMaxThreadsActive(threadingProfile.getMaxThreadsActive() + 1);
        workManager = threadingProfile.createWorkManager(getName());

        if (queueProfile == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            queueProfile = ((SedaModel) model).getQueueProfile();
        }
        
        if (queueTimeout == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            setQueueTimeout(((SedaModel) model).getQueueTimeout());
        }
        
        try
        {
            if (name == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Service has no name to identify it"), this);
            }
            // Setup event Queue (used for VM execution).  The queue has the same name as the service.
            queueProfile.configureQueue(getQueueName(), muleContext.getQueueManager());
            queue = muleContext.getQueueManager().getQueueSession().getQueue(getQueueName());
            if (queue == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Queue not created for service " + name), this);
            }
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            throw new InitialisationException(
                CoreMessages.objectFailedToInitialise("Service Queue"), e, this);
        }
    }

    protected void doForceStop() throws MuleException
    {
        doStop();
    }

    protected void doStop() throws MuleException
    {
        // Resume if paused. (This is required to unblock the "ticker" thread)
        if (isPaused())
        {
            paused.set(false);
        }

        if (queue != null && queue.size() > 0)
        {
            try
            {
                stopping.whenFalse(null);
            }
            catch (InterruptedException e)
            {
                // we can ignore this
                // TODO MULE-863: Why?
            }
        }
        workManager.dispose();
    }

    protected void doStart() throws MuleException
    {
        try
        {
            workManager.start();
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, this);
        }
        catch (Exception e)
        {
            throw new LifecycleException(
                CoreMessages.failedToStart("Service: " + name), e, this);
        }
    }

    protected void doDispose()
    {
        queue = null;
        // threadPool.awaitTerminationAfterShutdown();
        if (workManager != null)
        {
            workManager.dispose();
        }
    }

    protected void doDispatch(MuleEvent event) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("Service: {0} has received asynchronous event on: {1}",
                                              name, event.getEndpoint().getEndpointURI()));
        }

        // Block until we can queue the next event
        try
        {
            if (stats.isEnabled())
            {
                enqueue(event);
                synchronized (queueStatsGuard)
                {
                    stats.incQueuedEvent();
                }
            }
            else
            {
                // just enqueue without any hit for synchronization
                enqueue(event);
            }
        }
        catch (Exception e)
        {
            FailedToQueueEventException e1 = new FailedToQueueEventException(
                CoreMessages.interruptedQueuingEventFor(this.getName()), event.getMessage(), this, e);
            handleException(e1);
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("MuleEvent added to queue for: " + name);
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws MuleException
    {
        MuleMessage result = null;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(MessageFormat.format("{0} : got proxy for {1} = {2}", this, event.getId(), component));
            }
            Object replyTo = event.getMessage().getReplyTo();
            ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), (InboundEndpoint) event.getEndpoint());
            result = invokeComponent(event);
            result = sendToOutboundRouter(event, result);
            result = processAsyncReplyRouter(result);
            
            // Allow components to stop processing of the ReplyTo property (e.g. CXF)
            if (result != null && !BooleanUtils.toBoolean((String)result.getProperty(MuleProperties.MULE_REPLY_TO_STOP_PROPERTY)))
            {
                processReplyTo(event, result, replyToHandler, replyTo);
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MessagingException)
            {
                handleException(e);
            }
            else
            {
                handleException(new MessagingException(CoreMessages.eventProcessingFailedFor(getName()),
                    event.getMessage(), e));
            }
            if (result == null)
            {
                // important that we pull event from request context here as it may
                // have been modified
                // (necessary to avoid scribbling between threads)
                result = new DefaultMuleMessage(NullPayload.getInstance(), RequestContext.getEvent().getMessage());
            }
            ExceptionPayload exceptionPayload = result.getExceptionPayload();
            if (exceptionPayload == null)
            {
                exceptionPayload = new DefaultExceptionPayload(e);
            }
            result.setExceptionPayload(exceptionPayload);
        }
        return result;
    }

    public int getQueueSize()
    {
        if (queue == null)
        {
            logger.warn(new InitialisationException(MessageFactory.createStaticMessage("Queue not created for service " + name), this));
            return -1;
        }
        return queue.size();
    }
    
    private String getQueueName()
    {
        return name + QUEUE_NAME_SUFFIX;
    }

    /**
     * While the service isn't stopped this runs a continuous loop checking for new
     * events in the queue.
     */
    public void run()
    {
        DefaultMuleEvent event = null;
        QueueSession queueSession = muleContext.getQueueManager().getQueueSession();

        while (!stopped.get())
        {
            try
            {
                // Wait if the service is paused
                if (paused.get())
                {
                    paused.whenFalse(null);
                    
                    // If service is resumed as part of stopping 
                    if (stopping.get())
                    {
                        if (!queueProfile.isPersistent() && (queueSession != null && getQueueSize() > 0))
                        {
                            // Any messages in a non-persistent queue went paused service is stopped are lost
                            logger.warn(CoreMessages.stopPausedSedaServiceNonPeristentQueueMessageLoss(getQueueSize(), this));
                        }
                        stopping.set(false);
                        break;
                    }
                }

                // If we're doing a draining stop, read all events from the queue
                // before stopping
                if (stopping.get())
                {
                    if (queueProfile.isPersistent() || queueSession == null || getQueueSize() <= 0)
                    {
                        stopping.set(false);
                        break;
                    }
                }

                if (stats.isEnabled())
                {
                    event = (DefaultMuleEvent) dequeue();
                    if (event != null)
                    {
                        synchronized (queueStatsGuard)
                        {
                            stats.decQueuedEvent();
                        }
                    }
                }
                else
                {
                    // just dequeue without any hit for synchronization
                    event = (DefaultMuleEvent) dequeue();
                }

                if (event != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(MessageFormat.format("Service: {0} dequeued event on: {1}",
                                                          name, event.getEndpoint().getEndpointURI()));
                    }
                    Work work = new ComponentStageWorker(event);
                    if (threadingProfile.isDoThreading())
                    {
                        workManager.scheduleWork(work, WorkManager.INDEFINITE, null, this);
                    }
                    else
                    {
                        work.run();
                    }
                }
            }
            catch (Exception e)
            {
                if (e instanceof InterruptedException)
                {
                    stopping.set(false);
                    break;
                }
                if (e instanceof MuleException)
                {
                    handleException(e);
                }
                else
                {
                    handleException(
                        new ServiceException(
                            CoreMessages.eventProcessingFailedFor(name),
                            (event == null ? null : event.getMessage()), this, e));
                }
            }
        }
    }

    public void release()
    {
        stopping.set(false);
    }

    protected void enqueue(MuleEvent event) throws Exception
    {
        if (queue == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Queue not created for service " + name), this);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("Service {0} putting event on queue {1}: {2}", name, queue.getName(), event));
        }
        queue.put(event);
    }

    protected MuleEvent dequeue() throws Exception
    {
        if (queue == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Queue not created for service " + name), this);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("Service {0} polling queue {1}, timeout = {2}", name, queue.getName(), queueTimeout));
        }
        if (getQueueTimeout() == null)
        {
            throw new InitialisationException(CoreMessages.noServiceQueueTimeoutSet(this), this);
        }
        else
        {
            return (MuleEvent) queue.poll(getQueueTimeout());
        }
    }

    public void workAccepted(WorkEvent event)
    {
        handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event)
    {
        handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event)
    {
        handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event)
    {
        handleWorkException(event, "workCompleted");
    }

    protected void handleWorkException(WorkEvent event, String type)
    {
        Throwable e;

        if (event != null && event.getException() != null)
        {
            e = event.getException();
        }
        else
        {
            return;
        }

        if (event.getException().getCause() != null)
        {
            e = event.getException().getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                        + event.getWork().toString());

        if (e instanceof Exception)
        {
            handleException((Exception) e);
        }
        else
        {
            throw new MuleRuntimeException(
                CoreMessages.componentCausedErrorIs(this.getName()), e);
        }
    }

    protected ServiceStatistics createStatistics()
    {
        return new ServiceStatistics(getName(), threadingProfile.getMaxThreadsActive());
    }

    public Object getInstance() throws MuleException
    {
        throw new UnsupportedOperationException("Direct access to underlying service object is not allowed in the SedaModel.  If this is for a unit test, make sure you are using the TestSedaModel ('seda-test')");
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    public Integer getQueueTimeout()
    {
        return queueTimeout;
    }

    public void setQueueTimeout(Integer queueTimeout)
    {
        this.queueTimeout = queueTimeout;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    public WorkManager getWorkManager()
    {
        return workManager;
    }

    public void setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    protected void dispatchToOutboundRouter(MuleEvent event, MuleMessage result) throws MessagingException
    {
        super.dispatchToOutboundRouter(event, result);
        // TODO MULE-3077 SedaService should use a SEDA queue to dispatch to outbound
        // routers
    }

    private class ComponentStageWorker implements Work
    {
        private MuleEvent event;

        public ComponentStageWorker(MuleEvent event)
        {
            this.event = event;
        }

        public void run()
        {
            try
            {
                event = OptimizedRequestContext.criticalSetEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(),
                    (InboundEndpoint) event.getEndpoint());
                MuleMessage result = invokeComponent(event);
                dispatchToOutboundRouter(event, result);
                processReplyTo(event, result, replyToHandler, replyTo);
            }
            catch (Exception e)
            {
                event.getSession().setValid(false);
                if (e instanceof MessagingException)
                {
                    handleException(e);
                }
                else
                {
                    handleException(new MessagingException(CoreMessages.eventProcessingFailedFor(getName()),
                        event.getMessage(), e));
                }
            }
        }

        public void release()
        {
            // no-op
        }
    }
}
