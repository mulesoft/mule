/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.ThreadSafeAccess;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.transport.AbstractMessageRequester;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueSession;

import java.io.Serializable;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction
 * between components.
 */
public class VMMessageRequester extends AbstractMessageRequester
{

    private final VMConnector connector;

    public VMMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (VMConnector) endpoint.getConnector();
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *                The call should return immediately if there is data available. If
     *                no data becomes available before the timeout elapses, null will be
     *                returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        try
        {
            QueueSession queueSession = connector.getTransactionalResource(endpoint);
            Queue queue = queueSession.getQueue(endpoint.getEndpointURI().getAddress());

            if (queue == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("No queue with name " + endpoint.getEndpointURI().getAddress());
                }
                return null;
            }
            else
            {
                MuleMessage message = null;
                if (logger.isDebugEnabled())
                {
                    logger.debug("Waiting for a message on " + endpoint.getEndpointURI().getAddress());
                }
                try
                {
                    Serializable polledItem = queue.poll(timeout);

                    if (polledItem instanceof MuleEvent)
                    {
                        message = ((MuleEvent) polledItem).getMessage();
                    }
                    else
                    {
                        message = (MuleMessage) polledItem;
                    }
                }
                catch (InterruptedException e)
                {
                    logger.error("Failed to receive message from queue: " + endpoint.getEndpointURI());
                }
                if (message != null)
                {
                    //The message will contain old thread information, we need to reset it
                    if(message instanceof ThreadSafeAccess)
                    {
                        ((ThreadSafeAccess) message).resetAccessControl();
                    }
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Message received: " + message);
                    }
                    return message;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("No event received after " + timeout + " ms");
                    }
                    return null;
                }
            }
        }
        catch (Exception e)
        {
            throw e;
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
        // use the default queue profile to configure this queue.
        connector.getQueueProfile().configureQueue(
            getEndpoint().getMuleContext(), endpoint.getEndpointURI().getAddress(),
            connector.getQueueManager());
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
