/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.api.lifecycle.StopException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transport.AbstractMessageReceiver;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;
import org.mule.runtime.transport.jms.redelivery.RedeliveryHandler;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

public class TransactedSingleResourceJmsMessageReceiver extends AbstractMessageReceiver
        implements MessageListener
{
    protected JmsConnector connector;
    protected RedeliveryHandler redeliveryHandler;
    protected MessageConsumer consumer;
    protected Session session;
    protected boolean startOnConnect = false;

    /** determines whether messages will be received in a transaction template */
    protected boolean receiveMessagesInTransaction = true;

    /** determines whether Multiple receivers are created to improve throughput */
    protected boolean useMultipleReceivers = true;
    private final boolean topic;


    public TransactedSingleResourceJmsMessageReceiver(Connector connector,
                                                      FlowConstruct flowConstruct,
                                                      InboundEndpoint endpoint) throws CreateException
    {

        super(connector, flowConstruct, endpoint);

        this.connector = (JmsConnector) connector;
        topic = this.connector.getTopicResolver().isTopic(endpoint);
        // TODO check which properties being set in the TransecteJmsMessage receiver
        // are needed...

        try
        {
            redeliveryHandler = this.connector.getRedeliveryHandlerFactory().create();
            redeliveryHandler.setConnector(this.connector);
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
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
        try
        {
            JmsSupport jmsSupport = this.connector.getJmsSupport();
            // Create session if none exists
            if (session == null)
            {
                session = this.connector.getSession(endpoint);
            }

            // Create destination
            Destination dest = jmsSupport.createDestination(session, endpoint);
            // Extract jms selector
            String selector = null;
            JmsSelectorFilter selectorFilter = connector.getSelector(endpoint);
            if (selectorFilter != null)
            {
                selector = selectorFilter.getExpression();
            }
            else if (endpoint.getProperties() != null)
            {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatable
                selector = (String) endpoint.getProperties().get(JmsConstants.JMS_SELECTOR_PROPERTY);
            }
            String tempDurable = (String) endpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable).booleanValue();
            }

            // Get the durable subscriber name if there is one
            String durableName = (String) endpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY);
            if (durableName == null && durable && dest instanceof Topic)
            {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                        + durableName);
            }

            // Create consumer
            consumer = jmsSupport.createConsumer(session, dest, selector, connector.isNoLocal(), durableName,
                    topic, endpoint);
        }
        catch (JMSException e)
        {
            throw new ConnectException(e, this);
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            // We ned to register the listener when start is called in order to only
            // start receiving messages after start.
            // If the consumer is null it means that the connection strategy is being
            // run in a separate thread and hasn't managed to connect yet.
            if (consumer == null)
            {
                startOnConnect = true;
            }
            else
            {
                startOnConnect = false;
                this.consumer.setMessageListener(this);
            }
        }
        catch (JMSException e)
        {
            throw new StartException(e, this);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        try
        {
            if (consumer != null)
            {
                consumer.setMessageListener(null);
            }
        }
        catch (JMSException e)
        {
            throw new StopException(e, this);
        }
    }

    @Override
    public void doDisconnect() throws Exception
    {
        closeConsumer();
    }

    protected void closeConsumer()
    {
        connector.closeQuietly(consumer);
        consumer = null;
        connector.closeQuietly(session);
        session = null;
    }

    @Override
    public void onMessage(Message message)
    {
        try
        {
            processMessages(message, this);
            // Just in case we're not using AUTO_ACKNOWLEDGE (which is the default)
            message.acknowledge();
        }
        catch (MessagingException e)
        {
            //already handled by TransactionTemplate
            // This will cause a negative ack for JMS
            if (e.getEvent().getMessage().getExceptionPayload() != null)
            {
                throw new MuleRuntimeException(e);
            }
        }
        catch (Exception e)
        {
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            // This will cause a negative ack for JMS
            throw new MuleRuntimeException(e);
        }
    }

    public void processMessages(final Message message, final MessageReceiver receiver) throws Exception
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();

        final String encoding = endpoint.getEncoding();

        if (receiveMessagesInTransaction)
        {
            ExecutionCallback<MuleEvent> processingCallback = new MessageProcessingCallback<MuleEvent>(message)
            {

                @Override
                public MuleEvent process() throws Exception
                {
                    // Get Transaction & Bind MuleSession
                    Transaction tx = TransactionCoordination.getInstance().getTransaction();
                    if (tx != null)
                    {
                        tx.bindResource(connector.getConnection(), ReusableSessionWrapperFactory.createWrapper(session));
                    }
                    if (tx instanceof JmsClientAcknowledgeTransaction)
                    {
                        tx.bindResource(message, message);
                    }

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Message received it is of type: " +
                                ClassUtils.getSimpleName(message.getClass()));
                        if (message.getJMSDestination() != null)
                        {
                            logger.debug("Message received on " + message.getJMSDestination() + " ("
                                    + message.getJMSDestination().getClass().getName() + ")");
                        }
                        else
                        {
                            logger.debug("Message received on unknown destination");
                        }
                        logger.debug("Message CorrelationId is: " + message.getJMSCorrelationID());
                        logger.debug("Jms Message Id is: " + message.getJMSMessageID());
                    }

                    if (message.getJMSRedelivered())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Message with correlationId: "
                                    + message.getJMSCorrelationID()
                                    + " is redelivered. handing off to Exception Handler");
                        }
                        redeliveryHandler.handleRedelivery(message, receiver.getEndpoint(), receiver.getFlowConstruct());
                    }

                    MuleMessage messageToRoute = createMuleMessage(message, encoding);
                    return routeMessage(messageToRoute);
                }
            };
            executionTemplate.execute(processingCallback);
        }
        else
        {
            MuleMessage messageToRoute = createMuleMessage(message, encoding);
            routeMessage(messageToRoute);
        }
    }

    @Override
    public boolean shouldConsumeInEveryNode()
    {
        return !this.topic;
    }
}
