/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.impl.MuleMessage;
import org.mule.providers.ConnectException;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.commons.collections.MapUtils;

public class TransactedJmsMessageReceiver extends TransactedPollingMessageReceiver
{
    protected final JmsConnector connector;
    protected boolean reuseConsumer;
    protected boolean reuseSession;
    protected final ThreadContextLocal context = new ThreadContextLocal();
    protected final long timeout;
    protected final RedeliveryHandler redeliveryHandler;

    /**
     * Holder receiving the session and consumer for this thread.
     */
    protected static class JmsThreadContext
    {
        public Session session;
        public MessageConsumer consumer;
    }

    /**
     * Strongly typed ThreadLocal for ThreadContext.
     */
    protected static class ThreadContextLocal extends ThreadLocal
    {
        public JmsThreadContext getContext()
        {
            return (JmsThreadContext)get();
        }

        protected Object initialValue()
        {
            return new JmsThreadContext();
        }
    }

    public TransactedJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        // TODO AP: check how frequency=0 works with the scheduler, see setFrequency(long)
        super(connector, component, endpoint, 0);
        this.connector = (JmsConnector)connector;
        this.timeout = endpoint.getTransactionConfig().getTimeout();

        // If reconnection is set, default reuse strategy to false
        // as some jms brokers will not detect lost connections if the
        // same consumer / session is used
        if (this.connectionStrategy instanceof SingleAttemptConnectionStrategy)
        {
            this.reuseConsumer = true;
            this.reuseSession = true;
        }
        // User may override reuse strategy if necessary
        this.reuseConsumer = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseConsumer",
            this.reuseConsumer);
        this.reuseSession = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseSession",
            this.reuseSession);

        // Check if the destination is a queue and
        // if we are in transactional mode.
        // If true, set receiveMessagesInTransaction to true.
        // It will start multiple threads, depending on the threading profile.
        String resourceInfo = endpoint.getEndpointURI().getResourceInfo();
        boolean topic = (resourceInfo != null && "topic".equalsIgnoreCase(resourceInfo));

        // If we're using topics We dont want to use multiple receivers as we'll get
        // the same message
        // multiple times
        useMultipleReceivers = !topic;

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
        if (connector.isConnected())
        {
            // TODO Fix Bug

            // creating this consumer now would prevent from the actual worker
            // consumer
            // to receive the message!
        	//Antoine Borg 08 Dec 2006 - Uncommented for MULE-1150
             createConsumer();
            // if we comment this line, if one tries to restart the service through
            // JMX,
            // this will fail...
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (connector.isConnected())
        {
            closeConsumer(true);
        }
    }

    /**
     * The poll method is overriden from the {@link TransactedPollingMessageReceiver}
     */
    public void poll() throws Exception
    {
        try
        {
            JmsThreadContext ctx = context.getContext();
            // Create consumer if necessary
            if (ctx.consumer == null)
            {
                createConsumer();
            }
            // Do polling
            super.poll();
        }
        catch (Exception e)
        {
            // Force consumer to close
            closeConsumer(true);
            throw e;
        }
        finally
        {
            // Close consumer if necessary
            closeConsumer(false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.TransactionEnabledPollingMessageReceiver#getMessages()
     */
    protected List getMessages() throws Exception
    {
        // As the session is created outside the transaction, it is not
        // bound to it yet
        JmsThreadContext ctx = context.getContext();

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            tx.bindResource(connector.getConnection(), ctx.session);
        }

        // Retrieve message
        Message message = null;
        try
        {
            message = ctx.consumer.receive(timeout);
        }
        catch (JMSException e)
        {
            // If we're being disconnected, ignore the exception
            if (!this.isConnected())
            {
                // ignore
            }
            else
            {
                throw e;
            }
        }
        if (message == null)
        {
            if (tx != null)
            {
                tx.setRollbackOnly();
            }
            return null;
        }
        message = connector.preProcessMessage(message, ctx.session);

        // Process message
        if (logger.isDebugEnabled())
        {
            logger.debug("Message received it is of type: " + message.getClass().getName());
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
                logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                             + " is redelivered. handing off to Exception Handler");
            }
            redeliveryHandler.handleRedelivery(message);
        }

        if (tx instanceof JmsClientAcknowledgeTransaction)
        {
            tx.bindResource(message, null);
        }

        UMOMessageAdapter adapter = connector.getMessageAdapter(message);
        routeMessage(new MuleMessage(adapter));
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.TransactionEnabledPollingMessageReceiver#processMessage(java.lang.Object)
     */
    protected void processMessage(Object msg) throws Exception
    {
        // This method is never called as the
        // message is processed when received
    }

    protected void closeConsumer(boolean force)
    {
        JmsThreadContext ctx = context.getContext();
        if (ctx == null)
        {
            return;
        }
        // Close consumer
        if (force || !reuseSession || !reuseConsumer)
        {
            connector.closeQuietly(ctx.consumer);
            ctx.consumer = null;
        }
        // Do not close session if a transaction is in progress
        // the session will be close by the transaction
        if (force || !reuseSession)
        {
            connector.closeQuietly(ctx.session);
            ctx.session = null;
        }
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
            JmsThreadContext ctx = context.getContext();
            // Create session if none exists
            if (ctx.session == null)
            {
                ctx.session = this.connector.getSession(endpoint);
            }

            // Create destination
            String resourceInfo = endpoint.getEndpointURI().getResourceInfo();
            boolean topic = (resourceInfo != null && "topic".equalsIgnoreCase(resourceInfo));
            Destination dest = jmsSupport.createDestination(ctx.session, endpoint.getEndpointURI()
                .getAddress(), topic);

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
            String tempDurable = (String)endpoint.getProperties().get("durable");
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable).booleanValue();
            }

            // Get the durable subscriber name if there is one
            String durableName = (String)endpoint.getProperties().get("durableName");
            // TODO AP: to be replaced with a more intelligent topic detection,
            // instanceof does not always work as needed
            if (durableName == null && durable && dest instanceof Topic)
            {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                             + durableName);
            }

            // Create consumer
            ctx.consumer = jmsSupport.createConsumer(ctx.session, dest, selector, connector.isNoLocal(),
                durableName, topic);
        }
        catch (JMSException e)
        {
            throw new ConnectException(e, this);
        }
    }
}
