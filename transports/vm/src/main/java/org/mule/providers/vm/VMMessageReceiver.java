/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractPollingMessageReceiver;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.util.List;

/**
 * <code>VMMessageReceiver</code> is a listener of events from a mule component
 * which then simply passes the events on to the target component.
 */
public class VMMessageReceiver extends TransactedPollingMessageReceiver
{
    private VMConnector connector;
    private final Object lock = new Object();

    public VMMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint, AbstractPollingMessageReceiver.DEFAULT_POLL_FREQUENCY);
        this.connector = (VMConnector)connector;
        super.receiveMessagesInTransaction = endpoint.getTransactionConfig().isTransacted();
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        if (connector.isQueueEvents())
        {
            // Ensure we can create a vm queue
            QueueSession queueSession = connector.getQueueSession();
            Queue q = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            if (logger.isDebugEnabled())
            {
                logger.debug("Current queue depth for queue: " + endpoint.getEndpointURI().getAddress()
                             + " is: " + q.size());
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEventListener#onEvent(org.mule.umo.UMOEvent)
     */
    public void onEvent(UMOEvent event) throws UMOException
    {
        if (connector.isQueueEvents())
        {
            QueueSession queueSession = connector.getQueueSession();
            Queue queue = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            try
            {
                queue.put(event);
            }
            catch (InterruptedException e)
            {
                throw new MuleException(
                    CoreMessages.interruptedQueuingEventFor(this.endpoint.getEndpointURI()), e);
            }
        }
        else
        {
            UMOMessage msg = new MuleMessage(event.getTransformedMessage(), event.getMessage());
            synchronized (lock)
            {
                routeMessage(msg);
            }
        }
    }

    public Object onCall(UMOEvent event) throws UMOException
    {
        return routeMessage(new MuleMessage(event.getTransformedMessage(), event.getMessage()),
            event.isSynchronous());
    }

    protected List getMessages() throws Exception
    {
        QueueSession qs = connector.getQueueSession();
        Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
        UMOEvent event = (UMOEvent)queue.poll(connector.getQueueTimeout());
        if (event != null)
        {
            routeMessage(new MuleMessage(event.getTransformedMessage(), event.getMessage()));
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.TransactionEnabledPollingMessageReceiver#processMessage(java.lang.Object)
     */
    protected void processMessage(Object msg) throws Exception
    {
        // This method is never called as the message is processed when received
    }

}
