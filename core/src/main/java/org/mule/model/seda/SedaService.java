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
import org.mule.FailedToQueueEventException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.ServiceException;
import org.mule.component.AbstractComponent;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.ServiceStatistics;
import org.mule.service.AbstractService;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.util.NoSuchElementException;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

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

    /** For Spring only */
    public SedaService()
    {
        super();
    }
    
    /**
     * Initialise the service. The service will first create a Mule UMO from the
     * UMODescriptor and then initialise a pool based on the attributes in the
     * UMODescriptor.
     * 
     * @throws org.mule.api.lifecycle.InitialisationException if the service fails
     *             to initialise
     * @see org.mule.api.UMODescriptor
     */
    protected synchronized void doInitialise() throws InitialisationException
    {
        if (threadingProfile == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            threadingProfile = muleContext.getDefaultComponentThreadingProfile();
        }
        // Create thread pool
        workManager = threadingProfile.createWorkManager(getName());

        if (queueProfile == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            queueProfile = ((SedaModel) model).getQueueProfile();
        }
        
        if (queueTimeout == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            setQueueTimeout(new Integer(((SedaModel) model).getQueueTimeout()));
        }
        
        try
        {
            if (name == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Service has no name to identify it"), this);
            }
            // Setup event Queue (used for VM execution).  The queue has the same name as the service.
            queueProfile.configureQueue(name, muleContext.getQueueManager());
            queue = muleContext.getQueueManager().getQueueSession().getQueue(name);
            if (queue == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Queue " + name + " not created for service " + name), this);
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
        if (muleContext.getQueueManager().getQueueSession().getQueue(name).size() > 0)
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
        // threadPool.awaitTerminationAfterShutdown();
        if (workManager != null)
        {
            workManager.dispose();
        }
    }

    protected void doDispatch(MuleEvent event) throws MuleException
    {
        // Dispatching event to the service
        if (stats.isEnabled())
        {
            stats.incReceivedEventASync();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Service: " + name + " has received asynchronous event on: "
                         + event.getEndpoint().getEndpointURI());
        }

        // Block until we can queue the next event
        try
        {
            enqueue(event);
            if (stats.isEnabled())
            {
                stats.incQueuedEvent();
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
                logger.debug(this + " : got proxy for " + event.getId() + " = " + component);
            }
            result = component.onCall(event);
            // TODO MULE-3113
            // 1) Invoke component.onCall(event)
            // 2) Forward result to outbound routers ideally via a SEDA queue (MULE-3077)
            // 3) Process response router
            // 4) Process async reply-to
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException(event.getMessage(), this, e);
        }
        return result;
    }

    public int getQueueSize()
    {
        QueueSession session = muleContext.getQueueManager().getQueueSession();
        Queue queue = session.getQueue(name);
        if (queue == null)
        {
            logger.warn(new InitialisationException(MessageFactory.createStaticMessage("Queue " + name + " not created for service " + name), this));
            return -1;
        }
        return queue.size();
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
                paused.whenFalse(null);

                // If we're doing a draining stop, read all events from the queue
                // before stopping
                if (stopping.get())
                {
                    if (queueSession == null || getQueueSize() <= 0)
                    {
                        stopping.set(false);
                        break;
                    }
                }

                event = (DefaultMuleEvent) dequeue();
                if (event != null)
                {
                    if (stats.isEnabled())
                    {
                        stats.decQueuedEvent();
                    }

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Service: " + name + " dequeued event on: "
                                        + event.getEndpoint().getEndpointURI());
                    }
                    workManager.scheduleWork(new ComponentStageWorker(event), WorkManager.INDEFINITE, null, this);
                }
            }
            catch (Exception e)
            {
                if (isStopped() || isStopping())
                {
                    break;
                }

                if (e instanceof InterruptedException)
                {
                    stopping.set(false);
                    break;
                }
                else if (e instanceof NoSuchElementException)
                {
                    handleException(new ServiceException(CoreMessages.proxyPoolTimedOut(),
                        (event == null ? null : event.getMessage()), this, e));
                }
                else if (e instanceof MuleException)
                {
                    handleException(e);
                }
                else if (e instanceof WorkException)
                {
                    handleException(
                        new ServiceException(
                            CoreMessages.eventProcessingFailedFor(name),
                            (event == null ? null : event.getMessage()), this, e));
                }
                else
                {
                    handleException(
                        new ServiceException(
                            CoreMessages.failedToGetPooledObject(),
                            (event == null ? null : event.getMessage()), this, e));
                }
            }
            finally
            {
                stopping.set(false);
            }
        }
    }

    public void release()
    {
        stopping.set(false);
    }

    protected void enqueue(MuleEvent event) throws Exception
    {
        QueueSession session = muleContext.getQueueManager().getQueueSession();
        Queue queue = session.getQueue(name);
        if (queue == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Queue " + name + " not created for service " + name), this);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Service " + name + " putting event on queue " + name + ": " + event);
        }
        queue.put(event);
    }

    protected MuleEvent dequeue() throws Exception
    {
        if (logger.isDebugEnabled())
        {
            //logger.debug("Service " + name + " polling queue " + name + ", timeout = " + queueTimeout);
        }
        if (getQueueTimeout() == null)
        {
            throw new InitialisationException(CoreMessages.noServiceQueueTimeoutSet(this), this);
        }
        else
        {
            return (MuleEvent) queue.poll(getQueueTimeout().intValue());
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

    private class ComponentStageWorker implements Work
    {
        private MuleEvent event;

        public ComponentStageWorker(MuleEvent event)
        {
            this.event = event;
        }

        public void run()
        {
            ((AbstractComponent) component).onEvent(event);
            // TODO MULE-3113
            // 1) Invoke component.onCall(event)
            // 2) Forward result to outbound routers ideally via a SEDA queue (MULE-3077)
            // 3) Process async reply-to
        }

        public void release()
        {
            // no-op
        }
    }
}
