/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.Connector;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.transport.ConnectException;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.transport.jms.redelivery.RedeliveryHandler;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

public class XaTransactedJmsMessageReceiver extends TransactedPollingMessageReceiver
{
    public static final long DEFAULT_JMS_POLL_FREQUENCY = 100;
    public static final TimeUnit DEFAULT_JMS_POLL_TIMEUNIT = TimeUnit.MILLISECONDS;

    protected final JmsConnector connector;
    private final long localTimeout;
    protected boolean reuseConsumer;
    protected boolean reuseSession;
    protected final ThreadContextLocal context = new ThreadContextLocal();
    protected final long timeout;
    private final AtomicReference<RedeliveryHandler> redeliveryHandler = new AtomicReference<RedeliveryHandler>();
    private final boolean topic;

    @Override
    public boolean shouldConsumeInEveryNode() {
        return !this.topic;
    }

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
    protected static class ThreadContextLocal extends ThreadLocal<JmsThreadContext>
    {
        public JmsThreadContext getContext()
        {
            return get();
        }

        @Override
        protected JmsThreadContext initialValue()
        {
            return new JmsThreadContext();
        }
    }

    public XaTransactedJmsMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        // TODO AP: find appropriate value for polling frequency with the scheduler;
        // see setFrequency/setTimeUnit & VMMessageReceiver for more
        this.setTimeUnit(DEFAULT_JMS_POLL_TIMEUNIT);
        this.setFrequency(DEFAULT_JMS_POLL_FREQUENCY);

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

        // User may override reuse strategy if necessary. This is available for legacy reasons,
        // but this approach is not recommended and should never be set when using XA.
        this.reuseConsumer = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseConsumer",
            this.reuseConsumer);
        this.reuseSession = MapUtils.getBooleanValue(endpoint.getProperties(), "reuseSession",
            this.reuseSession);

        // Do extra validation, XA Topic & reuse are incompatible. See MULE-2622
        topic = this.connector.getTopicResolver().isTopic(getEndpoint());
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
        this.localTimeout = resolveReceiveTimeout();
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        if (redeliveryHandler.compareAndSet(null, connector.getRedeliveryHandlerFactory().create()))
        {
            redeliveryHandler.get().setConnector(this.connector);
        }
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
    @Override
    public void poll() throws Exception
    {
        logger.debug("Polling...");

        ExecutionTemplate<MuleEvent> processingCallback = createExecutionTemplate();
        ExecutionCallback<MuleEvent> cb = new ExecutionCallback<MuleEvent>()
        {
            @Override
            public MuleEvent process() throws Exception
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
                    if (ctx.consumer != null)
                    {
                        connector.closeQuietly(ctx.consumer);
                    }
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

        processingCallback.execute(cb);
    }

    private void handlePossibleDisconnectingException(Exception e) throws Exception
    {
        boolean isJMSException =
                (e instanceof JMSException) ||
                (e instanceof UndeclaredThrowableException &&
                 ((UndeclaredThrowableException) e).getUndeclaredThrowable().getCause() instanceof JMSException);
        if (isJMSException && !this.isConnected())
        {
            return; // If we're being disconnected, ignore the exception
        }
        throw e;
    }

    @Override
    protected List<MuleMessage> getMessages() throws Exception
    {
        Session session = this.connector.getTransactionalResource(endpoint);
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        MessageConsumer consumer = createConsumer();

        // Retrieve message
        Message message = null;
        try
        {
            message = consumer.receive(localTimeout);
        }
        catch (Exception e)
        {
            handlePossibleDisconnectingException(e);
        }

        if (message == null)
        {
            if (tx != null)
            {
                tx.setRollbackOnly();
            }
            closeConsumerIfRequired(consumer);
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
            redeliveryHandler.get().handleRedelivery(message, (InboundEndpoint) endpoint, flowConstruct);
        }

        MuleMessage messageToRoute = createMuleMessage(message, endpoint.getEncoding());
        routeMessage(messageToRoute);
        closeConsumerIfRequired(consumer);
        return null;
    }

    private long resolveReceiveTimeout()
    {
        long localTimeout = MapUtils.getLongValue(endpoint.getProperties(), "xaPollingTimeout", timeout);


        if (localTimeout > timeout)
        {
            logger.warn(String.format("Transaction timeout ('%s') must be greater than the timeout used for polling messages ('%s'). Using transaction timeout", localTimeout, timeout));
            localTimeout = timeout;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Consumer is receiving in '%s' ith timeout '%d'", this, localTimeout));
        }

        return localTimeout;
    }

    private void closeConsumerIfRequired(MessageConsumer consumer)
    {
        if (!this.reuseConsumer)
        {
            connector.closeQuietly(consumer);
            if (context.getContext() != null)
            {
                context.getContext().consumer = null;
            }
        }
    }

    @Override
    protected MuleEvent processMessage(Object msg) throws Exception
    {
        // This method is never called as the
        // message is processed when received
        return null;
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
            JmsSelectorFilter selectorFilter = connector.getSelector(endpoint);
            if (selectorFilter != null)
            {
                selector = selectorFilter.getExpression();
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
                durableName, topic, endpoint);
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
