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

import org.mule.impl.MuleMessage;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * <code>VMMessageReceiver</code> is a listener for events from a Mule component which then simply passes
 * the events on to the target component.
 */
public class VMMessageReceiver extends TransactedPollingMessageReceiver
{
    public static final long DEFAULT_VM_POLL_FREQUENCY = 10;
    public static final TimeUnit DEFAULT_VM_POLL_TIMEUNIT = TimeUnit.MILLISECONDS;

    private VMConnector connector;
    private final Object lock = new Object();

    public VMMessageReceiver(UMOConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
        this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().isTransacted());
        this.setFrequency(DEFAULT_VM_POLL_FREQUENCY);
        this.setTimeUnit(DEFAULT_VM_POLL_TIMEUNIT);
        this.connector = (VMConnector) connector;
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

    public void onMessage(UMOMessage message) throws UMOException
    {
        // Rewrite the message to treat it as a new message
        UMOMessage newMessage = new MuleMessage(message.getPayload(), message);

        // Is this lock required? (Leaving it in just in case)
        synchronized (lock)
        {
            routeMessage(newMessage);
        }
    }

    public Object onCall(UMOMessage message, boolean synchronous) throws UMOException
    {
        // Rewrite the message to treat it as a new message
        UMOMessage newMessage = new MuleMessage(message.getPayload(), message);
        return routeMessage(newMessage, synchronous);
    }

    protected List getMessages() throws Exception
    {
        // This "if" is required because polling takes place regardless of if queueEvents is true or not.
        // If queueEvents is false there is no queue for this endpoint and a NullPointerException would occur
        // See MULE-2781
        if (connector.isQueueEvents())
        {
            QueueSession qs = connector.getQueueSession();
            Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
            Object obj = queue.poll(connector.getQueueTimeout());
            UMOMessage message = (UMOMessage) obj;// queue.poll(connector.getQueueTimeout());
            if (message != null)
            {
                // Rewrite the message to treat it as a new message
                routeMessage(new MuleMessage(message.getPayload(), message));
            }
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
