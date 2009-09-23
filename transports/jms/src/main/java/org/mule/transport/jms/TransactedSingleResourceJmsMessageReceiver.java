/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.lifecycle.StopException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.ConnectException;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.util.ClassUtils;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.work.Work;

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

    
    public TransactedSingleResourceJmsMessageReceiver(Connector connector,
                                                      Service service,
                                                      InboundEndpoint endpoint) throws CreateException
    {

        super(connector, service, endpoint);

        this.connector = (JmsConnector) connector;

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

    protected void doDispose()
    {
        // template method
    }

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
            boolean topic = connector.getTopicResolver().isTopic(endpoint);

            Destination dest = jmsSupport.createDestination(session, endpoint);
            // Extract jms selector
            String selector = null;
            if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter)
            {
                selector = ((JmsSelectorFilter) endpoint.getFilter()).getExpression();
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

    protected void doStop() throws MuleException
    {
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

    public void onMessage(Message message)
    {
        try
        {
            getWorkManager().scheduleWork(new MessageReceiverWorker(message));
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    protected class MessageReceiverWorker implements Work
    {
        Message message;

        public MessageReceiverWorker(Message message)
        {
            this.message = message;
        }

        public void run()
        {
            try
            {
                TransactionTemplate tt = new TransactionTemplate(endpoint.getTransactionConfig(),
                        connector.getExceptionListener(), connector.getMuleContext());

                if (receiveMessagesInTransaction)
                {
                    TransactionCallback cb = new MessageTransactionCallback(message)
                    {

                        public Object doInTransaction() throws Exception
                        {
                            // Get Transaction & Bind MuleSession
                            Transaction tx = TransactionCoordination.getInstance().getTransaction();
                            if (tx != null)
                            {
                                tx.bindResource(connector.getConnection(), session);
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
                                redeliveryHandler.handleRedelivery(message);
                            }

                            MessageAdapter adapter = connector.getMessageAdapter(message);
                            routeMessage(new DefaultMuleMessage(adapter, connector.getMuleContext()));
                            return null;
                        }
                    };
                    tt.execute(cb);
                }
                else
                {
                    MessageAdapter adapter = connector.getMessageAdapter(message);
                    routeMessage(new DefaultMuleMessage(adapter, connector.getMuleContext()));
                }

            }
            catch (Exception e)
            {
                getConnector().handleException(e);
            }

        }

        public void release()
        {
            // Nothing to release.
        }

    }

}
