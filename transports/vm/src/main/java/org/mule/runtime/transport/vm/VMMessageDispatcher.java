/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transport.NoReceiverForEndpointException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueSession;
import org.mule.runtime.transport.vm.i18n.VMMessages;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction between components.
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
            throw new DispatchException(CoreMessages.objectIsNull("Endpoint"), event, getEndpoint());
        }
        MuleEvent eventToDispatch = DefaultMuleEvent.copy(event);
        eventToDispatch.clearFlowVariables();
        eventToDispatch.setMessage(eventToDispatch.getMessage().createInboundMessage());
        QueueSession session = getQueueSession();
        Queue queue = session.getQueue(endpointUri.getAddress());
        if (!queue.offer(eventToDispatch.getMessage(), connector.getQueueTimeout()))
        {
            // queue is full
            throw new DispatchException(VMMessages.queueIsFull(queue.getName(), queue.size()),
                eventToDispatch, getEndpoint());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatched MuleEvent on endpointUri: " + endpointUri);
        }
    }

    private QueueSession getQueueSession() throws MuleException
    {
        return connector.getTransactionalResource(endpoint);
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

        MuleEvent eventToSend = DefaultMuleEvent.copy(event);
        final MuleMessage message = eventToSend.getMessage().createInboundMessage();
        ExecutionTemplate<MuleMessage> executionTemplate = TransactionalExecutionTemplate.createTransactionalExecutionTemplate(
                event.getMuleContext(), receiver.getEndpoint().getTransactionConfig());
        ExecutionCallback<MuleMessage> processingCallback = new ExecutionCallback<MuleMessage>()
        {
            @Override
            public MuleMessage process() throws Exception
            {
                return receiver.onCall(message);
            }
        };
        retMessage = executionTemplate.execute(processingCallback);

        if (logger.isDebugEnabled())
        {
            logger.debug("sent event on endpointUri: " + endpoint.getEndpointURI());
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
            connector.getQueueProfile().configureQueue(endpoint.getMuleContext(),
                endpoint.getEndpointURI().getAddress(), connector.getQueueManager());
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
