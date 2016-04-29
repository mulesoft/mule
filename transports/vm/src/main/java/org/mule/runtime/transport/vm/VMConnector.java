/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.config.QueueProfile;
import org.mule.runtime.core.endpoint.DynamicURIInboundEndpoint;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.core.util.queue.QueueSession;

import java.util.Iterator;

/**
 * <code>VMConnector</code> A simple endpoint wrapper to allow a Mule service to
 * <p/> be accessed from an endpoint
 * 
 */
public class VMConnector extends AbstractConnector
{

    public static final String VM = "vm";
    private QueueProfile queueProfile;
    private Integer queueTimeout;

    public VMConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (queueTimeout == null)
        {
            queueTimeout = muleContext.getConfiguration().getDefaultQueueTimeout();
        }
        if (queueProfile == null)
        {
            // create a default QueueProfile
            queueProfile = QueueProfile.newInstancePersistingToDefaultMemoryQueueStore(muleContext);
            if (logger.isDebugEnabled())
            {
                logger.debug("created default QueueProfile for VM connector: " + queueProfile);
            }
        }
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        if (!endpoint.getExchangePattern().hasResponse())
        {
            queueProfile.configureQueue(endpoint.getMuleContext(), endpoint.getEndpointURI().getAddress(), getQueueManager());
        }
        return serviceDescriptor.createMessageReceiver(this, flowConstruct, endpoint);
    }

    @Override
    public String getProtocol()
    {
        return "VM";
    }

    /**
     * This implementation returns the default canonical representation
     * with an added query param specifying the connector name
     *
     * @param uri a not null {@link org.mule.api.endpoint.EndpointURI}
     * @return the canonical representation of the given uri as a {@link java.lang.String}
     */
    @Override
    public String getCanonicalURI(EndpointURI uri)
    {
        String canonicalURI = super.getCanonicalURI(uri);

        if (!canonicalURI.contains("?connector="))
        {
            canonicalURI = String.format("%s?connector=%s", canonicalURI, getName());
        }

        return canonicalURI;
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    VMMessageReceiver getReceiver(EndpointURI endpointUri) throws EndpointException
    {
        return (VMMessageReceiver)getReceiverByEndpoint(endpointUri);
    }

    QueueSession getQueueSession() throws InitialisationException
    {
        return getQueueManager().getQueueSession();
    }

    protected MessageReceiver getReceiverByEndpoint(EndpointURI endpointUri) throws EndpointException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Looking up vm receiver for address: " + endpointUri.toString());
        }

        MessageReceiver receiver;
        // If we have an exact match, use it
        receiver = receivers.get(endpointUri.getAddress());
        if (receiver != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Found exact receiver match on endpointUri: " + endpointUri);
            }
            return receiver;
        }

        // otherwise check each one against a wildcard match
        for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
        {
            receiver = (MessageReceiver)iterator.next();
            String filterAddress = receiver.getEndpointURI().getAddress();
            WildcardFilter filter = new WildcardFilter(filterAddress);
            if (filter.accept(endpointUri.getAddress()))
            {
                InboundEndpoint endpoint = receiver.getEndpoint();
                EndpointURI newEndpointURI = new MuleEndpointURI(endpointUri, filterAddress);
                receiver.setEndpoint(new DynamicURIInboundEndpoint(endpoint, newEndpointURI));

                if (logger.isDebugEnabled())
                {
                    logger.debug("Found receiver match on endpointUri: " + receiver.getEndpointURI()
                                 + " against " + endpointUri);
                }
                return receiver;
            }
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("No receiver found for endpointUri: " + endpointUri);
        }
        return null;
    }

    @Override
    public boolean isResponseEnabled()
    {
        return true;
    }                                                      

    public int getQueueTimeout()
    {
        return queueTimeout;
    }

    public void setQueueTimeout(int queueTimeout)
    {
        this.queueTimeout = queueTimeout;
    }

    public QueueManager getQueueManager()
    {
        return getMuleContext().getQueueManager();
    }

    @Override
    protected <T> T createOperationResource(ImmutableEndpoint endpoint) throws MuleException
    {
        return (T) getQueueManager().getQueueSession();
    }

    @Override
    protected <T> T getOperationResourceFactory()
    {
        return (T) getQueueManager();
    }
}
