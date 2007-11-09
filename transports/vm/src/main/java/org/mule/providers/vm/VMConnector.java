/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.DynamicEndpointURIEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.MessagingException;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueueSession;

import java.util.Iterator;

/**
 * <code>VMConnector</code> A simple endpoint wrapper to allow a Mule component to
 * <p/> be accessed from an endpoint
 * 
 */
public class VMConnector extends AbstractConnector
{

    public static final String VM = "vm";
    private boolean queueEvents = false;
    private QueueProfile queueProfile;
    private int queueTimeout = 1000;

    protected void doInitialise() throws InitialisationException
    {
        if (queueEvents)
        {
            if (queueProfile == null)
            {
                //create a default QueueProfile
                queueProfile = new QueueProfile();
                if(logger.isDebugEnabled())
                {
                    logger.debug("created default QueueProfile for VM connector: " + queueProfile);
                }
            }
        }

    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOImmutableEndpoint endpoint) throws Exception
    {
        if (queueEvents)
        {
            queueProfile.configureQueue(endpoint.getEndpointURI().getAddress(), managementContext.getQueueManager());
        }
        return serviceDescriptor.createMessageReceiver(this, component, endpoint);
    }

    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        if (message == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("message").getMessage());
        }
        else if (message instanceof MuleMessage)
        {
            return ((MuleMessage)message).getAdapter();
        }
        else if (message instanceof UMOMessageAdapter)
        {
            return (UMOMessageAdapter)message;
        }
        else
        {
            throw new MessagingException(CoreMessages.objectNotOfCorrectType(message.getClass(), UMOMessageAdapter.class), null);
        }
    }

    public String getProtocol()
    {
        return "VM";
    }

    public boolean isQueueEvents()
    {
        return queueEvents;
    }

    public void setQueueEvents(boolean queueEvents)
    {
        this.queueEvents = queueEvents;
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    VMMessageReceiver getReceiver(UMOEndpointURI endpointUri) throws EndpointException
    {
        return (VMMessageReceiver)getReceiverByEndpoint(endpointUri);
    }

    QueueSession getQueueSession() throws InitialisationException
    {
        QueueManager qm = managementContext.getQueueManager();
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            if (tx.hasResource(qm))
            {
                final QueueSession queueSession = (QueueSession) tx.getResource(qm);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Retrieved VM queue session " + queueSession + " from current transaction " + tx);
                }
                return queueSession;
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Retrieving new VM queue session from queue manager");
        }

        QueueSession session = qm.getQueueSession();
        if (tx != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Binding VM queue session " + session + " to current transaction " + tx);
            }
            try
            {
                tx.bindResource(qm, session);
            }
            catch (TransactionException e)
            {
                throw new RuntimeException("Could not bind queue session to current transaction", e);
            }
        }
        return session;
    }

    protected UMOMessageReceiver getReceiverByEndpoint(UMOEndpointURI endpointUri) throws EndpointException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Looking up vm receiver for address: " + endpointUri.toString());
        }

        UMOMessageReceiver receiver;
        // If we have an exact match, use it
        receiver = (UMOMessageReceiver)receivers.get(endpointUri.getAddress());
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
            receiver = (UMOMessageReceiver)iterator.next();
            String filterAddress = receiver.getEndpointURI().getAddress();
            WildcardFilter filter = new WildcardFilter(filterAddress);
            if (filter.accept(endpointUri.getAddress()))
            {
                UMOImmutableEndpoint endpoint = receiver.getEndpoint();
                UMOEndpointURI newEndpointURI = new MuleEndpointURI(endpointUri, filterAddress);
                receiver.setEndpoint(new DynamicEndpointURIEndpoint(endpoint, newEndpointURI));

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

    // //@Override
    public boolean isRemoteSyncEnabled()
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

}
