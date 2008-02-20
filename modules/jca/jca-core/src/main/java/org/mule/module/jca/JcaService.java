/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.ObjectNotFoundException;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.jca.i18n.JcaMessages;
import org.mule.service.AbstractService;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * <code>JcaService</code> Is the type of service used in Mule when embedded inside an app server using
 * JCA. In the future we might want to use one of the existing models.
 */
public class JcaService extends AbstractService implements WorkListener
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1510441245219710451L;

    protected WorkManager workManager;

    public JcaService(WorkManager workManager)
    {
        super();
        this.workManager = workManager;
    }

    /**
     * This is the synchronous call method and not supported by components managed in a JCA container
     * 
     * @param event
     * @return
     * @throws MuleException
     */
    public MuleMessage sendEvent(MuleEvent event) throws MuleException
    {
        throw new UnsupportedOperationException("sendEvent()");
    }

    public boolean isPaused()
    {
        // JcaService is a wrapper for a hosted service implementation and
        // therefore cannot be paused by mule
        return false;
    }

    protected void waitIfPaused(MuleEvent event) throws InterruptedException
    {
        // JcaService is a wrapper for a hosted service implementation and
        // therefore cannot be paused by mule
    }

    protected void doPause() throws MuleException
    {
        throw new ServiceException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    protected void doResume() throws MuleException
    {
        throw new ServiceException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    protected synchronized void doInitialise() throws InitialisationException
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = model.getEntryPointResolverSet();
        }
    }

    protected void doDispatch(MuleEvent event) throws MuleException
    {
        try
        {
            workManager.scheduleWork(new MuleJcaWorker(event), WorkManager.INDEFINITE, null, this);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(CoreMessages.failedToInvoke("UMO Service: " + getName()), e);
        }
    }

    /**
     * Implementation of template method which is never call because send() is overwritten
     */
    protected MuleMessage doSend(MuleEvent event) throws MuleException
    {
        return null;
    }

    /*
     * The service ins actually managed by the Application Server container,Since the instance might be
     * pooled by the server, we should use the MessageEndPointFactory to delegate the request for creation to
     * the container. The container might create a Proxy object to intercept the actual method call to
     * implement transaction,security related functionalities
     */
    public Object getManagedInstance() throws MuleException
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

        private MuleEvent event;

        MuleJcaWorker(MuleEvent event)
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
                logger.trace("MuleJcaWorker: async MuleEvent for Mule  JCA EndPoint " + getName());
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
