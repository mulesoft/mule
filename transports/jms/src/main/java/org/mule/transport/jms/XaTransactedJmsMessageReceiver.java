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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.XaTransaction;
import org.mule.transport.ConnectException;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;

import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class XaTransactedJmsMessageReceiver extends TransactedPollingMessageReceiver
{
    public static final long DEFAULT_JMS_POLL_FREQUENCY = 100;
    public static final TimeUnit DEFAULT_JMS_POLL_TIMEUNIT = TimeUnit.MILLISECONDS;
    
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

    public XaTransactedJmsMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, service, endpoint);
        // TODO AP: find appropriate value for polling frequency with the scheduler;
        // see setFrequency/setTimeUnit & VMMessageReceiver for more
        this.setTimeUnit(DEFAULT_JMS_POLL_TIMEUNIT);
        this.setFrequency(MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency", DEFAULT_JMS_POLL_FREQUENCY));

        this.connector = (JmsConnector) connector;
        this.timeout = endpoint.getTransactionConfig().getTimeout();

        // If reconnection is configured, default reuse strategy to false
        // as some jms brokers will not detect lost connections if the
        // same consumer / session is used
        if (retryTemplate != null && !(retryTemplate instanceof NoRetryPolicyTemplate))
        {
            this.reuseConsumer = false;
            this.reuseSession = false;
        }

        // User may override reuse strategy if necessary
        this.reuseConsumer = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseConsumer",
            this.reuseConsumer);
        this.reuseSession = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseSession",
            this.reuseSession);

        // Do extra validation, XA Topic & reuse are incompatible. See MULE-2622
        boolean topic = this.connector.getTopicResolver().isTopic(getEndpoint());
        if (topic && (reuseConsumer || reuseSession))
        {
            logger.warn("Destination " + getEndpoint().getEndpointURI() + " is a topic and XA transaction was " +
                        "configured. Forcing 'reuseSession' and 'reuseConsumer' to false. Set these " +
                        "on endpoint to avoid the message.");
            reuseConsumer = false;
            reuseSession = false;
        }

        // Check if the destination is a queue and
        // if we are in transactional mode.
        // If true, set receiveMessagesInTransaction to true.
        // It will start multiple threads, depending on the threading profile.

        // If we're using topics we don't want to use multiple receivers as we'll get
        // the same message multiple times
        this.setUseMultipleTransactedReceivers(!topic);

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
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        if (connector.isConnected())
        {
            // TODO All resources will be close by transaction or by connection close
            closeResource(true);
        }
    }

    /**
     * The poll method is overriden from the {@link TransactedPollingMessageReceiver}
     */
    public void poll() throws Exception
    {
        logger.debug("Polling...");
        
        TransactionTemplate tt = new TransactionTemplate(endpoint.getTransactionConfig(), 
            connector.getExceptionListener(), connector.getMuleContext());
        TransactionCallback cb = new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                try
                {
                    List messages = getMessages();
                    if (messages != null && messages.size() > 0)
                    {
                        for (Object message : messages)
                        {
                            processMessage(message);
                        }
                    }
                    return null;
                }
                catch (Exception e)
                {
                    // There is not a need to close resources here,
                    // they will be close by XaTransaction,  
                    JmsThreadContext ctx = context.getContext();
                    ctx.consumer = null;
                    Transaction tx = TransactionCoordination.getInstance().getTransaction();
                    if (ctx.session != null && tx instanceof XaTransaction.MuleXaObject)
                    {
                        if (ctx.session instanceof XaTransaction.MuleXaObject)
                        {
                            ((XaTransaction.MuleXaObject) ctx.session).setReuseObject(false);
                        }
                        else
                        {
                            logger.warn("Session should be XA, but is of type " + ctx.session.getClass().getName());
                        }
                    }
                    ctx.session = null;
                    throw e;
                }
            }
        };
        
        tt.execute(cb);
    }

    protected List getMessages() throws Exception
    {
        Session session = this.connector.getSessionFromTransaction();
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        MessageConsumer consumer = createConsumer();

        // Retrieve message
        Message message = null;
        try
        {
            message = consumer.receive(timeout);
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
        message = connector.preProcessMessage(message, session);

        // Process message
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
                logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                             + " is redelivered. handing off to Exception Handler");
            }
            redeliveryHandler.handleRedelivery(message);
        }

        MessageAdapter adapter = connector.getMessageAdapter(message);
        routeMessage(new DefaultMuleMessage(adapter, (Map) null));
        return null;
    }

    protected void processMessage(Object msg) throws Exception
    {
        // This method is never called as the
        // message is processed when received
    }

    /**
     * Close Sesison and consumer
     */
    protected void closeResource(boolean force)
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
        // the session will be closed by the transaction
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
    protected MessageConsumer createConsumer() throws Exception
    {
        logger.debug("Create a consumer for the jms destination");
        try
        {
            JmsSupport jmsSupport = this.connector.getJmsSupport();

            JmsThreadContext ctx = context.getContext();
            if (ctx == null)
            {
                ctx = new JmsThreadContext();
            }
            
            Session session;
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (this.reuseSession && ctx.session != null)
            {
                session = ctx.session;
                tx.bindResource(this.connector.getConnection(), session);
            }
            else
            {
                session = this.connector.getSession(endpoint);
                if (session != null && tx != null)
                {
                    if (session instanceof XaTransaction.MuleXaObject)
                    {
                        ((XaTransaction.MuleXaObject) session).setReuseObject(reuseSession);
                    }
                    else
                    {
                        logger.warn("Session should be XA, but is of type " + session.getClass().getName());
                    }
                }
            }
            
            if (reuseSession)
            {
                ctx.session = session;
            }
            
            // TODO How can I verify that the consumer is active?
            if (this.reuseConsumer && ctx.consumer != null)
            {
                return ctx.consumer;
            }

            // Create destination
            final boolean topic = connector.getTopicResolver().isTopic(endpoint);
            Destination dest = jmsSupport.createDestination(session, endpoint);

            // Extract jms selector
            String selector = null;
            if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter)
            {
                selector = ((JmsSelectorFilter)endpoint.getFilter()).getExpression();
            }
            else if (endpoint.getProperties() != null)
            {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatible
                selector = (String)endpoint.getProperties().get(JmsConstants.JMS_SELECTOR_PROPERTY);
            }
            String tempDurable = (String)endpoint.getProperties().get("durable");
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable);
            }

            // Get the durable subscriber name if there is one
            String durableName = (String)endpoint.getProperties().get("durableName");
            if (durableName == null && durable && topic)
            {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                             + durableName);
            }

            // Create consumer
            MessageConsumer consumer = jmsSupport.createConsumer(session, dest, selector, connector.isNoLocal(),
                durableName, topic);
            if (reuseConsumer)
            {
                ctx.consumer = consumer;
            }
            return consumer;
        }
        catch (JMSException e)
        {
            throw new ConnectException(e, this);
        }
    }
}
