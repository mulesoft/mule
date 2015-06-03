/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.ContinuousPollingReceiverWorker;
import org.mule.transport.PollingReceiverWorker;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * <code>VMMessageReceiver</code> is a listener for events from a Mule service which then simply passes
 * the events on to the target service.
 */
public class VMMessageReceiver extends TransactedPollingMessageReceiver
{

    private VMConnector connector;

    public VMMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().isTransacted());
        this.connector = (VMConnector) connector;
    }

    /*
     * We only need to start scheduling this receiver if event queueing is enabled on the connector; otherwise
     * events are delivered via onEvent/onCall.
     */
    @Override
    protected void schedule() throws RejectedExecutionException, NullPointerException, IllegalArgumentException
    {
        if (!endpoint.getExchangePattern().hasResponse())
        {
            super.schedule();
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
        if (!endpoint.getExchangePattern().hasResponse())
        {
            // Ensure we can create a vm queue
            QueueSession queueSession = connector.getQueueSession();
            Queue q = queueSession.getQueue(endpoint.getEndpointURI().getAddress());
            if (logger.isDebugEnabled())
            {
                logger.debug("Current queue depth for queue: " + endpoint.getEndpointURI().getAddress() + " is: "
                             + q.size());
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    public void onMessage(MuleMessage message) throws MuleException
    {
        // Rewrite the message to treat it as a new message
        MuleMessage newMessage = new DefaultMuleMessage(message.getPayload(), message, endpoint.getMuleContext());
        routeMessage(newMessage);
    }

    public MuleMessage onCall(final MuleMessage message) throws MuleException
    {

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        //TODO remove code to change message MuleContext once MULE-7357 gets fixed
        MuleContext originaMuleContext = message.getMuleContext();
        try
        {
            Thread.currentThread().setContextClassLoader(endpoint.getMuleContext().getExecutionClassLoader());
            ((DefaultMuleMessage)message).setMuleContext(originaMuleContext);
            ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
            MuleEvent resultEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    MuleEvent event = routeMessage(message);
                    MuleMessage returnedMessage = !getEndpoint().getExchangePattern().hasResponse() || event == null ? null : event.getMessage();
                    if (returnedMessage != null)
                    {
                        returnedMessage.release();
                    }
                    return event;
                }
            });
            if (resultEvent != null)
            {
                DefaultMuleMessage resultMessage = (DefaultMuleMessage) resultEvent.getMessage();
                resultMessage.setMuleContext(originaMuleContext);
                return resultMessage;
            }
            else
            {
                return null;
            }
        }
        catch (MessagingException e)
        {
            //Already handled by TransactionTemplate, return ES result
            return e.getMuleMessage();
        }
        catch (MuleException e)
        {
            endpoint.getMuleContext().getExceptionListener().handleException(e);
            throw e;
        }
        catch (Exception e)
        {
            endpoint.getMuleContext().getExceptionListener().handleException(e);
            throw new DefaultMuleException(e);
        }
        finally
        {
            ((DefaultMuleMessage) message).setMuleContext(originaMuleContext);
            message.release();
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * It's impossible to process all messages in the receive transaction
     */
    @Override
    protected List<MuleMessage> getMessages() throws Exception
    {
        if (isReceiveMessagesInTransaction())
        {
            MuleEvent message = getFirstMessage();
            if (message == null)
            {
                return null;
            }
            
            List<MuleMessage> messages = new ArrayList<MuleMessage>(1);
            ((DefaultMuleMessage)message.getMessage()).setMuleContext(endpoint.getMuleContext());
            messages.add(message.getMessage());
            return messages;
        }
        else
        {
            return getFirstMessages();
        }
    }
    
    protected List<MuleMessage> getFirstMessages() throws Exception
    {
        // The queue from which to pull events
        QueueSession qs = connector.getTransactionalResource(endpoint);
        Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());

        // The list of retrieved messages that will be returned
        List<MuleMessage> messages = new LinkedList<MuleMessage>();

        int batchSize = getBatchSize(queue.size());

        // try to get the first event off the queue
        MuleEvent message = (MuleEvent) queue.poll(connector.getQueueTimeout());

        if (message != null)
        {
            // keep first dequeued event
            ((DefaultMuleMessage)message.getMessage()).setMuleContext(endpoint.getMuleContext());
            messages.add(message.getMessage());

            // keep batching if more events are available
            for (int i = 0; i < batchSize && message != null; i++)
            {
                message = (MuleEvent)queue.poll(0);
                if (message != null)
                {
                    messages.add(new DefaultMuleMessage(message.getMessage(), endpoint.getMuleContext()));
                }
            }
        }

        // let our workManager handle the batch of events
        return messages;
    }
    
    protected MuleEvent getFirstMessage() throws Exception
    {
        // The queue from which to pull events
        QueueSession qs = connector.getTransactionalResource(endpoint);
        Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
        // try to get the first event off the queue
        return (MuleEvent) queue.poll(connector.getQueueTimeout());
    }

    @Override
    protected boolean hasNoMessages()
    {
        try
        {
            QueueSession qs = connector.getQueueSession();
            Queue queue = qs.getQueue(endpoint.getEndpointURI().getAddress());
            return queue.size() == 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    protected MuleEvent processMessage(Object msg) throws Exception
    {
        MuleMessage message = (MuleMessage) msg;

        if (message instanceof ThreadSafeAccess)
        {
            message = (MuleMessage)((ThreadSafeAccess) message).newThreadCopy();
        }
        return routeMessage(message);
    }

    /*
     * We create our own "polling" worker here since we need to evade the standard scheduler.
     */
    @Override
    protected PollingReceiverWorker createWork()
    {
        return new ContinuousPollingReceiverWorker(this);
    }    
}
