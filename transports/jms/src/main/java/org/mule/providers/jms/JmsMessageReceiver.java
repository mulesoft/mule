/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.AbstractReceiverWorker;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Registers a single JmsMessage listener but uses a thread pool to process incoming
 * messages
 */
public class JmsMessageReceiver extends AbstractMessageReceiver implements MessageListener
{

    protected JmsConnector connector;
    protected RedeliveryHandler redeliveryHandler;
    protected MessageConsumer consumer;
    protected Session session;
    protected boolean startOnConnect = false;

    public JmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
        this.connector = (JmsConnector) connector;

        try
        {
            redeliveryHandler = (RedeliveryHandler) this.connector.getRedeliveryHandler().getOrCreate();
            redeliveryHandler.setConnector(this.connector);
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    protected void doConnect() throws Exception
    {
        createConsumer();
        if (startOnConnect)
        {
            doStart();
        }
    }

    protected void doDisconnect() throws Exception
    {
        closeConsumer();
    }

    public void onMessage(Message message)
    {
        try
        {
            getWorkManager().scheduleWork(new JmsWorker(message, this));
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    protected  class JmsWorker extends AbstractReceiverWorker
    {
        public JmsWorker(Message message, AbstractMessageReceiver receiver)
        {
            super(new ArrayList(1), receiver);
            messages.add(message);
        }

        public JmsWorker(List messages, AbstractMessageReceiver receiver)
        {
            super(messages, receiver);
        }

        //@Override
        protected Object preProcessMessage(Object message) throws Exception
        {
            Message m = (Message) message;

            if (logger.isDebugEnabled())
            {
                logger.debug("Message received it is of type: " +
                        ClassUtils.getSimpleName(message.getClass()));
                if (m.getJMSDestination() != null)
                {
                    logger.debug("Message received on " + m.getJMSDestination() + " ("
                            + m.getJMSDestination().getClass().getName() + ")");
                }
                else
                {
                    logger.debug("Message received on unknown destination");
                }
                logger.debug("Message CorrelationId is: " + m.getJMSCorrelationID());
                logger.debug("Jms Message Id is: " + m.getJMSMessageID());
            }

            if (m.getJMSRedelivered() && redeliveryHandler != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Message with correlationId: " + m.getJMSCorrelationID()
                            + " has redelivered flag set, handing off to Exception Handler");
                }
                redeliveryHandler.handleRedelivery(m);
            }
            return m;

        }

        protected void bindTransaction(UMOTransaction tx) throws TransactionException
        {
            if(tx instanceof JmsTransaction)
            {
                tx.bindResource(connector.getConnection(), session);
            }
            else if(tx instanceof JmsClientAcknowledgeTransaction)
            {
                //We should still bind the session to the transaction, but we also need the message itself
                //since that is the object that gets Acknowledged
                tx.bindResource(connector.getConnection(), session);
                ((JmsClientAcknowledgeTransaction)tx).setMessage((Message)messages.get(0));
            }
        }
    }

    protected void doStart() throws UMOException
    {
        try
        {
            // We ned to register the listener when start is called in order to only
            // start receiving messages after
            // start/
            // If the consumer is null it means that the connection strategy is being
            // run in a separate thread
            // And hasn't managed to connect yet.
            if (consumer == null)
            {
                startOnConnect = true;
            }
            else
            {
                startOnConnect = false;
                consumer.setMessageListener(this);
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

    protected void doDispose()
    {
        // template method
    }

    protected void closeConsumer()
    {
        connector.closeQuietly(consumer);
        consumer = null;
        connector.closeQuietly(session);
        session = null;
    }

    /**
     * Create a consumer for the jms destination
     *
     * @throws Exception
     */
    protected void createConsumer() throws Exception
    {
        try
        {
            JmsSupport jmsSupport = this.connector.getJmsSupport();
            // Create session if none exists
            if (session == null)
            {
                session = this.connector.getSession(endpoint);
            }

            boolean topic = connector.getTopicResolver().isTopic(endpoint, true);

            // Create destination
            Destination dest = jmsSupport.createDestination(session, endpoint.getEndpointURI().getAddress(),
                    topic);

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
                    topic);
        }
        catch (JMSException e)
        {
            throw new ConnectException(e, this);
        }
    }

}
