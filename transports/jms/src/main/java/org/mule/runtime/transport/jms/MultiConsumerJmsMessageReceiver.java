/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.transaction.TransactionCollection;
import org.mule.runtime.core.transport.AbstractMessageReceiver;
import org.mule.runtime.core.transport.AbstractReceiverWorker;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;
import org.mule.runtime.transport.jms.reconnect.ReconnectWorkManager;
import org.mule.runtime.transport.jms.redelivery.RedeliveryHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * In Mule an endpoint corresponds to a single receiver. It's up to the receiver to do multithreaded consumption and
 * resource allocation, if needed. This class honors the <code>numberOfConcurrentTransactedReceivers</code> strictly
 * and will create exactly this number of consumers.
 */
public class MultiConsumerJmsMessageReceiver extends AbstractMessageReceiver
{
    protected final List<SubReceiver> consumers;

    protected final int receiversCount;

    private final JmsConnector jmsConnector;

    final boolean isTopic;

    private final ReconnectWorkManager reconnectWorkManager;
    private boolean reconnecting = false;
    private boolean started = false;

    public MultiConsumerJmsMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);

        jmsConnector = (JmsConnector) connector;

        isTopic = jmsConnector.getTopicResolver().isTopic(endpoint, true);
        if (isTopic && jmsConnector.getNumberOfConsumers() != 1)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Destination " + getEndpoint().getEndpointURI() + " is a topic, but " + jmsConnector.getNumberOfConsumers() +
                                " receivers have been requested. Will configure only 1.");
            }
            receiversCount = 1;
        }
        else
        {
            receiversCount = jmsConnector.getNumberOfConsumers();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating " + receiversCount + " sub-receivers for " + endpoint.getEndpointURI());
        }

        consumers = new CopyOnWriteArrayList<SubReceiver>();
        reconnectWorkManager = new ReconnectWorkManager(getEndpoint().getMuleContext());
    }


    @Override
    protected synchronized void doStart() throws MuleException
    {
        started = true;
        this.reconnectWorkManager.startIfNotStarted();
        if (!connected.get())
        {
            try
            {
                connect();
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
        else
        {
            startSubReceivers();
        }
    }


    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        logger.debug("doStop()");
        if (consumers != null)
        {
            SubReceiver sub;
            for (Iterator<SubReceiver> it = consumers.iterator(); it.hasNext();)
            {
                sub = it.next();
                sub.doStop(true);
            }
        }
        reconnectWorkManager.dispose();
    }

    @Override
    protected synchronized void doConnect() throws Exception
    {
        logger.debug("doConnect()");
        if (reconnecting)
        {
            return;
        }
        reconnecting = true;

        reconnectWorkManager.startIfNotStarted();
        retryTemplate.execute(new RetryCallback()
        {
            @Override
            public void doWork(RetryContext context) throws Exception
            {
                try
                {
                    logger.debug("doConnect()");
                    if (!consumers.isEmpty())
                    {
                        if (consumers.get(0).connected)
                        {
                            context.setOk();
                            reconnecting = false;
                            return;
                        }
                        throw new IllegalStateException("List should be empty, there may be a concurrency issue here (see EE-1275)");
                    }

                    SubReceiver sub;
                    for (int i = 0; i < receiversCount; i++)
                    {
                        sub = new SubReceiver();
                        sub.doConnect();
                        consumers.add(sub);
                    }
                    if (started)
                    {
                        startSubReceivers();
                    }
                    context.setOk();
                    logger.info("Endpoint " + endpoint.getEndpointURI() + " has been successfully reconnected.");
                    reconnecting = false;
                }
                catch (Exception e)
                {
                    throw new Exception("Fail to connect", e);
                }
            }

            @Override
            public String getWorkDescription()
            {
                return getConnectionDescription();
            }

            @Override
            public Connector getWorkOwner()
            {
                return jmsConnector;
            }
        }, reconnectWorkManager);
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        logger.debug("doDisconnect()");

        SubReceiver sub;
        for (Iterator<SubReceiver> it = consumers.iterator(); it.hasNext();)
        {
            sub = it.next();
            try
            {
                sub.doDisconnect();
            }
            finally
            {
                sub = null;
            }
        }
        consumers.clear();
    }

    @Override
    protected void doDispose()
    {
        logger.debug("doDispose()");
    }

    protected void startSubReceivers() throws MuleException
    {
        SubReceiver sub;
        for (Iterator<SubReceiver> it = consumers.iterator(); it.hasNext(); )
        {
            sub = it.next();
            sub.doStart();
        }
    }

    @Override
    public boolean shouldConsumeInEveryNode()
    {
        return !this.isTopic;
    }

    protected class SubReceiver implements MessageListener
    {
        private final Log subLogger = LogFactory.getLog(getClass());

        private volatile Session session;
        private volatile MessageConsumer consumer;

        protected volatile boolean connected;
        protected volatile boolean started;
        protected volatile boolean isProcessingMessage;

        protected void doConnect() throws MuleException
        {
            subLogger.debug("SUB doConnect()");
            try
            {
                createConsumer();
            }
            catch (Exception e)
            {
                throw new LifecycleException(e, this);
            }
            connected = true;
        }

        protected void doDisconnect() throws MuleException
        {
            subLogger.debug("SUB doDisconnect()");
            if (started)
            {
                doStop(true);
            }
            closeConsumer();
            connected = false;
        }

        protected void closeConsumer()
        {
            jmsConnector.closeQuietly(consumer);
            consumer = null;
            if (isProcessingMessage)
            {
                recoverSession();
            }
            jmsConnector.closeQuietly(session);
            session = null;
        }

        private void recoverSession()
        {
            try
            {
                //If it's processing a message then don't lose it
                session.recover();
            }
            catch (Exception jmsEx)
            {
                logger.error(jmsEx);
            }
        }

        protected void doStart() throws MuleException
        {
            subLogger.debug("SUB doStart()");
            if (!connected)
            {
                doConnect();
            }

            try
            {
                MessageListener currentMessageListener = consumer.getMessageListener();
                if (currentMessageListener == null || currentMessageListener != this)
                {
                    consumer.setMessageListener(this);
                }
                started = true;
            }
            catch (JMSException e)
            {
                throw new LifecycleException(e, this);
            }
        }

        /**
         * Stop the subreceiver.
         * @param force - if true, any exceptions will be logged but the subreceiver will be considered stopped regardless
         * @throws MuleException only if force = false
         */
        protected void doStop(boolean force) throws MuleException
        {
            subLogger.debug("SUB doStop()");

            if (consumer != null)
            {
                try
                {
                    consumer.setMessageListener(null);
                    started = false;
                }
                catch (JMSException e)
                {
                    if (force)
                    {
                        logger.warn("Unable to cleanly stop subreceiver: " + e.getMessage());
                        started = false;
                    }
                    else
                    {
                        throw new LifecycleException(e, this);
                    }
                }
            }
        }

        /**
         * Create a consumer for the jms destination.
         */
        protected void createConsumer() throws Exception
        {
            subLogger.debug("SUB createConsumer()");

            boolean sessionCreated = false;
            try
            {
                JmsSupport jmsSupport = jmsConnector.getJmsSupport();
                boolean topic = jmsConnector.getTopicResolver().isTopic(endpoint, true);

                // Create session if none exists
                if (session == null)
                {
                    sessionCreated = true;
                    session = jmsConnector.getSession(endpoint);
                }

                // Create destination
                Destination dest = jmsSupport.createDestination(session, endpoint);

                // Extract jms selector
                String selector = null;
                JmsSelectorFilter selectorFilter = jmsConnector.getSelector(endpoint);
                if (selectorFilter != null)
                {
                    selector = selectorFilter.getExpression();
                }
                else
                {
                    if (endpoint.getProperties() != null)
                    {
                        // still allow the selector to be set as a property on the endpoint
                        // to be backward compatable
                        selector = (String) endpoint.getProperties().get(JmsConstants.JMS_SELECTOR_PROPERTY);
                    }
                }
                String tempDurable = (String) endpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY);
                boolean durable = jmsConnector.isDurable();
                if (tempDurable != null)
                {
                    durable = Boolean.valueOf(tempDurable);
                }

                // Get the durable subscriber name if there is one
                String durableName = (String) endpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY);
                if (durableName == null && durable && topic)
                {
                    durableName = "mule." + jmsConnector.getName() + "." + endpoint.getEndpointURI().getAddress();
                    logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                                 + durableName);
                }

                // Create consumer
                try
                {
                    // Create consumer
                    consumer = jmsSupport.createConsumer(session, dest, selector, jmsConnector.isNoLocal(), durableName,
                                                                                          topic, endpoint);
                }
                catch (Exception e)
                {
                    if (sessionCreated)
                    {
                        jmsConnector.closeQuietly(session);
                    }
                    throw e;
                }
            }
            catch (JMSException e)
            {
                throw new ConnectException(e, MultiConsumerJmsMessageReceiver.this);
            }
        }

        @Override
        public void onMessage(final Message message)
        {
            try
            {
                isProcessingMessage = true;
                // Note: Despite the name "Worker", there is no new thread created here in order to maintain synchronicity for exception handling.
                JmsWorker worker = new JmsWorker(message, MultiConsumerJmsMessageReceiver.this, this);
                worker.processMessages();
            }
            catch (Exception e)
            {
                // Use this rollback method in case a transaction has not been configured on the endpoint.
                RollbackSourceCallback rollbackMethod = new RollbackSourceCallback()
                {
                    @Override
                    public void rollback()
                    {
                        recoverSession();
                    }
                };

                if (e instanceof MessagingException)
                {
                    MessagingException messagingException = (MessagingException) e;
                    if (!messagingException.getEvent().isTransacted() && messagingException.causedRollback())
                    {
                        rollbackMethod.rollback();
                    }
                }
                else
                {
                    getEndpoint().getMuleContext().getExceptionListener().handleException(e, rollbackMethod);
                }
            }
            finally
            {
                isProcessingMessage = false;
            }
        }
    }

    protected class JmsWorker extends AbstractReceiverWorker
    {
        private final SubReceiver subReceiver;

        public JmsWorker(Message message, AbstractMessageReceiver receiver, SubReceiver subReceiver)
        {
            super(new ArrayList<Object>(1), receiver);
            this.subReceiver = subReceiver;
            messages.add(message);
        }

        @Override
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

            if (m.getJMSRedelivered())
            {
                // lazily create the redelivery handler
                RedeliveryHandler redeliveryHandler = jmsConnector.getRedeliveryHandlerFactory().create();
                redeliveryHandler.setConnector(jmsConnector);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Message with correlationId: " + m.getJMSCorrelationID()
                                 + " has redelivered flag set, handing off to Redelivery Handler");
                }
                redeliveryHandler.handleRedelivery(m, receiver.getEndpoint(), receiver.getFlowConstruct());
            }
            return m;

        }

        @Override
        protected void bindTransaction(Transaction tx) throws TransactionException
        {
            if (tx instanceof JmsTransaction || tx instanceof TransactionCollection)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Binding " + subReceiver.session + " to " + jmsConnector.getConnection());
                }
                tx.bindResource(jmsConnector.getConnection(), ReusableSessionWrapperFactory.createWrapper(subReceiver.session));
            }
            else
            {
                if (tx instanceof JmsClientAcknowledgeTransaction)
                {
                    //We should still bind the session to the transaction, but we also need the message itself
                    //since that is the object that gets Acknowledged
                    //tx.bindResource(jmsConnector.getConnection(), session);
                    ((JmsClientAcknowledgeTransaction) tx).setMessage((Message) messages.get(0));
                }
            }
        }
    }

}
