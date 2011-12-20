/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.TransactionTemplateFactory;
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

    @Override
    protected void doDispatch(final MuleEvent event) throws Exception
    {
        EndpointURI endpointUri = endpoint.getEndpointURI();

        if (endpointUri == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("Endpoint"), event,
                getEndpoint());
        }
        MuleMessage newMessage = event.getMessage().createInboundMessage();
        MuleEvent newEvent = new DefaultMuleEvent(newMessage, event);
        QueueSession session = connector.getQueueSession();
        Queue queue = session.getQueue(endpointUri.getAddress());
        if (!queue.offer(newEvent, connector.getQueueTimeout()))
        {
            // queue is full
            throw new DispatchException(VMMessages.queueIsFull(queue.getName(), queue.size()),
                    newEvent, getEndpoint());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatched MuleEvent on endpointUri: " + endpointUri);
        }
    }

    @Override
    protected MuleMessage doSend(final MuleEvent event) throws Exception
    {
        MuleMessage retMessage;
        final VMMessageReceiver receiver = connector.getReceiver(endpoint.getEndpointURI());
        // Apply any outbound transformers on this event before we dispatch

        if (receiver == null)
        {
            throw new NoReceiverForEndpointException(VMMessages.noReceiverForEndpoint(connector.getName(),
                                                                                      endpoint.getEndpointURI()));
        }

        final MuleMessage message = event.getMessage().createInboundMessage();
        TransactionTemplate<MuleMessage> tt = TransactionTemplateFactory.<MuleMessage>createNestedTransactionTemplate(
                                                            receiver.getEndpoint().getTransactionConfig(),
                                                            event.getMuleContext());

        TransactionCallback<MuleMessage> cb = new TransactionCallback<MuleMessage>()
        {
            public MuleMessage doInTransaction() throws Exception
            {
                return receiver.onCall(message);
            }
        };
        retMessage = tt.execute(cb);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("sent event on endpointUri: " +endpoint.getEndpointURI());
        }
        if (retMessage != null)
        {
            retMessage = retMessage.createInboundMessage();
        }
        return retMessage;
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        if (!endpoint.getExchangePattern().hasResponse())
        {
            // use the default queue profile to configure this queue.
            connector.getQueueProfile().configureQueue(
                    connector.getMuleContext(), endpoint.getEndpointURI().getAddress(), connector.getQueueManager());
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
