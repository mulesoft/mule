/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
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

    /**
     * @param connector
     * @param component
     * @param endpoint
     * @throws InitialisationException
     */
    public TransactedSingleResourceJmsMessageReceiver(UMOConnector connector,
                                                      UMOComponent component,
                                                      UMOEndpoint endpoint) throws InitialisationException
    {
        super(connector, component, endpoint);
        this.connector = (JmsConnector)connector;

        // TODO check which properties being set in the TransecteJmsMessage receiver
        // are needed...

        try
        {
            redeliveryHandler = this.connector.createRedeliveryHandler();
            redeliveryHandler.setConnector(this.connector);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
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
            boolean topic = connector.getTopicResolver().isTopic(endpoint, true);

            Destination dest = jmsSupport.createDestination(session, endpoint.getEndpointURI().getAddress(),
                topic);

            // Extract jms selector
            String selector = null;
            if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter)
            {
                selector = ((JmsSelectorFilter)endpoint.getFilter()).getExpression();
            }
            else if (endpoint.getProperties() != null)
            {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatable
                selector = (String)endpoint.getProperties().get(JmsConstants.JMS_SELECTOR_PROPERTY);
            }
            String tempDurable = (String)endpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable).booleanValue();
            }

            // Get the durable subscriber name if there is one
            String durableName = (String)endpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY);
            if (durableName == null && durable && dest instanceof Topic)
            {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                             + durableName);
            }

            // Create consumer
            consumer = jmsSupport.createConsumer(session, dest, selector, connector.isNoLocal(), durableName,
                topic);
        }
        catch (JMSException e)
        {
            throw new ConnectException(e, this);
        }
    }

    protected void doStart() throws UMOException
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
            throw new LifecycleException(e, this);
        }
    }

    protected void doStop() throws UMOException
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
            throw new LifecycleException(e, this);
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
                    connector.getExceptionListener(), connector.getManagementContext());

                if (receiveMessagesInTransaction)
                {
                    TransactionCallback cb = new MessageTransactionCallback(message)
                    {

                        public Object doInTransaction() throws Exception
                        {
                            // Get Transaction & Bind Session
                            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
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

                            UMOMessageAdapter adapter = connector.getMessageAdapter(message);
                            routeMessage(new MuleMessage(adapter));
                            return null;
                        }
                    };
                    tt.execute(cb);
                }
                else
                {
                    UMOMessageAdapter adapter = connector.getMessageAdapter(message);
                    routeMessage(new MuleMessage(adapter));
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
