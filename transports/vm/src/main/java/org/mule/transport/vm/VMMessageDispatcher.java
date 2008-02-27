/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.vm.i18n.VMMessages;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction
 * between components.
 */
public class VMMessageDispatcher extends AbstractMessageDispatcher
{
    private final VMConnector connector;

    public VMMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (VMConnector) endpoint.getConnector();
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        EndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        //Apply any outbound transformers on this event before we dispatch
        event.transformMessage();

        if (endpointUri == null)
        {
            throw new DispatchException(
                    CoreMessages.objectIsNull("Endpoint"), event.getMessage(), event.getEndpoint());
        }
        if (connector.isQueueEvents())
        {
            QueueSession session = connector.getQueueSession();
            Queue queue = session.getQueue(endpointUri.getAddress());
            queue.put(event.getMessage());
        }
        else
        {
            VMMessageReceiver receiver = connector.getReceiver(event.getEndpoint().getEndpointURI());
            if (receiver == null)
            {
                logger.warn("No receiver for endpointUri: " + event.getEndpoint().getEndpointURI());
                return;
            }
            MuleMessage message = event.getMessage(); 
            connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), message);
            receiver.onMessage(message);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatched MuleEvent on endpointUri: " + endpointUri);
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        MuleMessage retMessage;
        EndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        VMMessageReceiver receiver = connector.getReceiver(endpointUri);
        //Apply any outbound transformers on this event before we dispatch
        event.transformMessage();
        if (receiver == null)
        {
            if (connector.isQueueEvents())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Writing to queue as there is no receiver on connector: "
                            + connector.getName() + ", for endpointUri: "
                            + event.getEndpoint().getEndpointURI());
                }
                doDispatch(event);
                return null;
            }
            else
            {
                throw new NoReceiverForEndpointException(
                        VMMessages.noReceiverForEndpoint(connector.getName(),
                                event.getEndpoint().getEndpointURI()));
            }
        }

        MuleMessage message = event.getMessage(); 
        connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), message);
        retMessage = (MuleMessage) receiver.onCall(message, event.isSynchronous());

        if (logger.isDebugEnabled())
        {
            logger.debug("sent event on endpointUri: " + event.getEndpoint().getEndpointURI());
        }
        return retMessage;
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        if (connector.isQueueEvents())
        {
            // use the default queue profile to configure this queue.
            connector.getQueueProfile().configureQueue(
                    endpoint.getEndpointURI().getAddress(), connector.getQueueManager());
        }
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
