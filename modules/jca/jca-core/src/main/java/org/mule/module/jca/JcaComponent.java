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
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.service.Service;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.module.jca.i18n.JcaMessages;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

public class JcaComponent extends AbstractJavaComponent implements WorkListener
{
    protected MessageEndpointFactory messageEndpointFactory;
    protected WorkManager workManager;

    public JcaComponent(MessageEndpointFactory messageEndpointFactory,
                        EntryPointResolverSet entryPointResolverSet,
                        Service service,
                        WorkManager workManager)
    {
        this.messageEndpointFactory = messageEndpointFactory;
        this.entryPointResolverSet = entryPointResolverSet;
        this.service = service;
        this.workManager = workManager;
    }

    /*
     * The service ins actually managed by the Application Server container,Since the
     * instance might be pooled by the server, we should use the
     * MessageEndPointFactory to delegate the request for creation to the container.
     * The container might create a Proxy object to intercept the actual method call
     * to implement transaction,security related functionalities
     */
    public Object getManagedInstance() throws UnavailableException, MuleException
    {
        return messageEndpointFactory.createEndpoint(null);
    }

    // @Override
    public Object doInvoke(MuleEvent event)
    {
        try
        {
            workManager.scheduleWork(new MuleJcaWorker(event), WorkManager.INDEFINITE, null, this);
        }
        catch (Exception e)
        {
            logger.error(CoreMessages.failedToInvoke("Service: " + service.getName()));
        }
        return null;
    }

    public Class getObjectType()
    {
        return MessageEndpoint.class;
    }

    // @Override
    protected LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception
    {
        // Template method unused because doOnCall and doOnEvent have been overridden
        return null;
    }

    // @Override
    protected void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter)
    {
        // Template method unused because doOnCall and doOnEvent have been overridden
    }

    // @Override
    protected void doInitialise() throws InitialisationException
    {
        // no-op no object-factory
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
                logger.trace("MuleJcaWorker: async MuleEvent for Mule  JCA EndPoint " + service.getName());
            }
            try
            {
                // Invoke method
                event = OptimizedRequestContext.criticalSetEvent(event);
                entryPointResolverSet.invoke(getManagedInstance(), RequestContext.getEventContext());
            }
            catch (Exception e)
            {
                if (e instanceof UnavailableException)
                {
                    Message message = JcaMessages.cannotAllocateManagedInstance();
                    logger.error(message);
                    service.getExceptionListener().exceptionThrown(new MessagingException(message, e));
                }
                else if (e instanceof MessagingException)
                {
                    logger.error("Failed to execute  JCAEndPoint " + e.getMessage(), e);
                    service.getExceptionListener().exceptionThrown(e);
                }
                else
                {
                    service.getExceptionListener().exceptionThrown(
                        new MessagingException(CoreMessages.eventProcessingFailedFor(service.getName()), e));
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
