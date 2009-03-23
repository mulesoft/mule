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

import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.MessageFactory;
import org.mule.transaction.TransactionCollection;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractReceiverWorker;
import org.mule.transport.ConnectException;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.resource.spi.work.WorkException;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingDeque;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * In Mule an endpoint corresponds to a single receiver. It's up to the receiver to do multithreaded consumption and
 * resource allocation, if needed. This class honors the <code>numberOfConcurrentTransactedReceivers</code> strictly
 * and will create exactly this number of consumers.
 */
public class MultiConsumerJmsMessageReceiver extends AbstractMessageReceiver
{
    protected final BlockingDeque consumers;

    protected volatile int receiversCount;

    private final JmsConnector jmsConnector;

    public MultiConsumerJmsMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);

        jmsConnector = (JmsConnector) connector;

        final boolean isTopic = jmsConnector.getTopicResolver().isTopic(endpoint, true);
        receiversCount = jmsConnector.getNumberOfConsumers();
        if (isTopic && receiversCount != 1)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Destination " + getEndpoint().getEndpointURI() + " is a topic, but " + receiversCount +
                                " receivers have been requested. Will configure only 1.");
            }
            receiversCount = 1;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating " + receiversCount + " sub-receivers for " + endpoint.getEndpointURI());
        }
        consumers = new LinkedBlockingDeque(receiversCount);
    }

    @Override
    protected void doStart() throws MuleException
    {
        logger.debug("doStart()");
        SubReceiver sub;
        for (Iterator<SubReceiver> it = consumers.iterator(); it.hasNext();)
        {
            sub = it.next();
            sub.doStart();
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
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
    }

    @Override
    protected void doConnect() throws Exception
    {
        logger.debug("doConnect()");

        SubReceiver sub;
        for (int i = 0; i < receiversCount; i++)
        {
            sub = new SubReceiver();
            sub.doConnect();
            consumers.add(sub);
        }
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

    private class SubReceiver implements MessageListener
    {
        private final Log subLogger = LogFactory.getLog(getClass());

        private volatile Session session;
        private volatile MessageConsumer consumer;

        protected volatile boolean connected;
        protected volatile boolean started;
        
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
            jmsConnector.closeQuietly(session);
            session = null;
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
                consumer.setMessageListener(this);
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
            
            try
            {
                JmsSupport jmsSupport = jmsConnector.getJmsSupport();
                boolean topic = jmsConnector.getTopicResolver().isTopic(endpoint, true);

                // Create session if none exists
                if (session == null)
                {
                    session = jmsConnector.getSession(endpoint);
                }

                // Create destination
                Destination dest = jmsSupport.createDestination(session, endpoint);

                // Extract jms selector
                String selector = null;
                if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter)
                {
                    selector = ((JmsSelectorFilter) endpoint.getFilter()).getExpression();
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
                consumer = jmsSupport.createConsumer(session, dest, selector, jmsConnector.isNoLocal(), durableName,
                                                     topic);
            }
            catch (JMSException e)
            {
                throw new ConnectException(e, MultiConsumerJmsMessageReceiver.this);
            }
        }

        public void onMessage(final Message message)
        {
            try
            {
                // This must be the doWork() to preserve the transactional context.
                // We are already running in the consumer thread by this time.
                // The JmsWorker classe is a one-off executor which is abandoned after it's done and is
                // easily garbage-collected (confirmed with a profiler)
                getWorkManager().doWork(new JmsWorker(message, MultiConsumerJmsMessageReceiver.this, this));
            }
            catch (WorkException e)
            {
                throw new MuleRuntimeException(MessageFactory.createStaticMessage(
                        "Couldn't submit a work item to the WorkManager"), e);
            }
        }
    }

    protected class JmsWorker extends AbstractReceiverWorker
    {
        private final SubReceiver subReceiver;

        public JmsWorker(Message message, AbstractMessageReceiver receiver, SubReceiver subReceiver)
        {
            super(new ArrayList(1), receiver);
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
                                 + " has redelivered flag set, handing off to Exception Handler");
                }
                redeliveryHandler.handleRedelivery(m);
            }
            return m;

        }

        protected void bindTransaction(Transaction tx) throws TransactionException
        {
            if (tx instanceof JmsTransaction || tx instanceof TransactionCollection)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Binding " + subReceiver.session + " to " + jmsConnector.getConnection());
                }
                tx.bindResource(jmsConnector.getConnection(), subReceiver.session);
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
