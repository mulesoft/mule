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

import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.vm.i18n.VMMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.NoReceiverForEndpointException;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction
 * between components.
 */
public class VMMessageDispatcher extends AbstractMessageDispatcher
{
    private final VMConnector connector;

    public VMMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (VMConnector) endpoint.getConnector();
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        //Apply any outbound transformers on this event before we dispatch
        event.getTransformedMessage();

        if (endpointUri == null)
        {
            throw new DispatchException(
                    CoreMessages.objectIsNull("Endpoint"), event.getMessage(), event.getEndpoint());
        }
        if (connector.isQueueEvents())
        {
            QueueSession session = connector.getQueueSession();
            Queue queue = session.getQueue(endpointUri.getAddress());
            queue.put(event);
        }
        else
        {
            VMMessageReceiver receiver = connector.getReceiver(event.getEndpoint().getEndpointURI());
            if (receiver == null)
            {
                logger.warn("No receiver for endpointUri: " + event.getEndpoint().getEndpointURI());
                return;
            }

            receiver.onEvent(event);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatched Event on endpointUri: " + endpointUri);
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        UMOMessage retMessage;
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        VMMessageReceiver receiver = connector.getReceiver(endpointUri);
        //Apply any outbound transformers on this event before we dispatch
        event.getTransformedMessage();
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

        retMessage = (UMOMessage) receiver.onCall(event);

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
                    endpoint.getEndpointURI().getAddress(), connector.getManagementContext().getQueueManager());
        }
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
