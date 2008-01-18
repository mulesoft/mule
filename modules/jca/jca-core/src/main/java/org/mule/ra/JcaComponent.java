/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.RequestContext;
import org.mule.impl.model.AbstractComponent;
import org.mule.ra.i18n.JcaMessages;
import org.mule.umo.ComponentException;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOWorkManager;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

/**
 * <code>JcaComponent</code> Is the type of component used in Mule when embedded inside an app server using
 * JCA. In the future we might want to use one of the existing models.
 */
public class JcaComponent extends AbstractComponent implements WorkListener
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1510441245219710451L;

    protected UMOWorkManager workManager;

    public JcaComponent(UMOWorkManager workManager)
    {
        super();
        this.workManager = workManager;
    }

    /**
     * This is the synchronous call method and not supported by components managed in a JCA container
     * 
     * @param event
     * @return
     * @throws UMOException
     */
    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        throw new UnsupportedOperationException("sendEvent()");
    }

    public boolean isPaused()
    {
        // JcaComponent is a wrapper for a hosted component implementation and
        // therefore cannot be paused by mule
        return false;
    }

    protected void waitIfPaused(UMOEvent event) throws InterruptedException
    {
        // JcaComponent is a wrapper for a hosted component implementation and
        // therefore cannot be paused by mule
    }

    protected void doPause() throws UMOException
    {
        throw new ComponentException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    protected void doResume() throws UMOException
    {
        throw new ComponentException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    public synchronized void doInitialise() throws InitialisationException
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = model.getEntryPointResolverSet();
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException
    {
        try
        {
            workManager.scheduleWork(new MuleJcaWorker(event), WorkManager.INDEFINITE, null, this);
        }
        catch (Exception e)
        {
            throw new MuleException(CoreMessages.failedToInvoke("UMO Component: " + getName()), e);
        }
    }

    /**
     * Implementation of template method which is never call because send() is overwritten
     */
    protected UMOMessage doSend(UMOEvent event) throws UMOException
    {
        return null;
    }

    /*
     * The component ins actually managed by the Application Server container,Since the instance might be
     * pooled by the server, we should use the MessageEndPointFactory to delegate the request for creation to
     * the container. The container might create a Proxy object to intercept the actual method call to
     * implement transaction,security related functionalities
     */
    public Object getManagedInstance() throws UMOException
    {
        Object managedInstance = null;
        try
        {

            MessageEndpointFactory messageEndpointFactory = (MessageEndpointFactory) getOrCreateService();
            managedInstance = messageEndpointFactory.createEndpoint(null);
        }
        catch (UnavailableException e)
        {

            logger.error("Request Failed to allocate Managed Instance" + e.getMessage(), e);
            throw new ObjectNotFoundException(this.getName(), e);
        }
        return managedInstance;
    }

    public class MuleJcaWorker implements Work
    {

        private UMOEvent event;

        MuleJcaWorker(UMOEvent event)
        {
            this.event = event;
        }

        public void release()
        {
            // TODO Auto-generated method stub
        }

        public void run()
        {

            if (logger.isTraceEnabled())
            {
                logger.trace("MuleJcaWorker: async Event for Mule  JCA EndPoint " + getName());
            }
            try
            {
                // Invoke method
                event = OptimizedRequestContext.criticalSetEvent(event);
                entryPointResolverSet.invoke(getManagedInstance(), RequestContext.getEventContext());
            }
            catch (Exception e)
            {
                if (e instanceof MessagingException)
                {
                    logger.error("Failed to execute  JCAEndPoint " + e.getMessage(), e);
                    handleException(e);
                }
                else
                {
                    handleException(new MessagingException(CoreMessages.eventProcessingFailedFor(getName()), e));
                }
            }
        }
    }

    public void workAccepted(WorkEvent arg0)
    {
        // TODO Auto-generated method stub
    }

    public void workCompleted(WorkEvent arg0)
    {
        // TODO Auto-generated method stub
    }

    public void workRejected(WorkEvent arg0)
    {
        // TODO Auto-generated method stub
    }

    public void workStarted(WorkEvent arg0)
    {
        // TODO Auto-generated method stub
    }
}
