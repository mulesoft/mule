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
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.jms.i18n.JmsMessages;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.ClassUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;
import org.mule.util.concurrent.WaitableBoolean;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages to JMS
 * destinations. All JMS semantics apply and settings such as replyTo and QoS
 * properties are read from the event properties or defaults are used (according to
 * the JMS specification)
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher
{

    private JmsConnector connector;
    private Session cachedSession;

    public JmsMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JmsConnector)endpoint.getConnector();
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        dispatchMessage(event);
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    private UMOMessage dispatchMessage(UMOEvent event) throws Exception
    {
        Session session = null;
        MessageProducer producer = null;
        MessageConsumer consumer = null;
        Destination replyTo = null;
        boolean transacted = false;
        boolean cached = false;
        boolean remoteSync = useRemoteSync(event);

        if (logger.isDebugEnabled())
        {
            logger.debug("dispatching on endpoint: " + event.getEndpoint().getEndpointURI()
                         + ". Event id is: " + event.getId());
        }

        try
        {
            // Retrieve the session from the current transaction.
            // TODO AP: clean up to use getDelegateSession()
            session = connector.getSessionFromTransaction();
            if (session != null)
            {
                transacted = true;

                // If a transaction is running, we can not receive any messages
                // in the same transaction.
                if (remoteSync)
                {
                    throw new IllegalTransactionStateException(
                        JmsMessages.connectorDoesNotSupportSyncReceiveWhenTransacted());
                }
            }
            // Should we be caching sessions? Note this is not part of the JMS spec.
            // and is turned off by default.
            else if (event.getMessage().getBooleanProperty(JmsConstants.CACHE_JMS_SESSIONS_PROPERTY,
                connector.isCacheJmsSessions()))
            {
                cached = true;
                if (cachedSession != null)
                {
                    session = cachedSession;
                }
                else
                {
                    // Retrieve a session from the connector
                    // TODO AP: clean up to use getDelegateSession()
                    session = connector.getSession(event.getEndpoint());
                    cachedSession = session;
                }
            }
            else
            {
                // Retrieve a session from the connector
                // TODO AP: clean up to use getDelegateSession()
                session = connector.getSession(event.getEndpoint());
                if (event.getEndpoint().getTransactionConfig().isTransacted())
                {
                    transacted = true;
                }
            }

            UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

            boolean topic = false;
            String resourceInfo = endpointUri.getResourceInfo();
            topic = (resourceInfo != null && JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(resourceInfo));
            // TODO MULE20 remove resource info support
            if (!topic)
            {
                topic = MapUtils.getBooleanValue(event.getEndpoint().getProperties(),
                    JmsConstants.TOPIC_PROPERTY, false);
            }

            Destination dest = connector.getJmsSupport().createDestination(session, endpointUri.getAddress(),
                topic);
            producer = connector.getJmsSupport().createProducer(session, dest, topic);

            Object message = event.getTransformedMessage();
            if (!(message instanceof Message))
            {
                throw new DispatchException(
                    JmsMessages.checkTransformer("JMS message", message.getClass(), connector.getName()),
                    event.getMessage(), event.getEndpoint());
            }

            Message msg = (Message)message;
            if (event.getMessage().getCorrelationId() != null)
            {
                msg.setJMSCorrelationID(event.getMessage().getCorrelationId());
            }

            UMOMessage eventMsg = event.getMessage();

            // Some JMS implementations might not support the ReplyTo property.
            if (connector.supportsProperty(JmsConstants.JMS_REPLY_TO))
            {
                Object tempReplyTo = eventMsg.removeProperty(JmsConstants.JMS_REPLY_TO);
                if (tempReplyTo != null)
                {
                    if (tempReplyTo instanceof Destination)
                    {
                        replyTo = (Destination)tempReplyTo;
                    }
                    else
                    {
                        boolean replyToTopic = false;
                        String reply = tempReplyTo.toString();
                        int i = reply.indexOf(":");
                        if (i > -1)
                        {
                            // TODO MULE-1409 this check will not work for ActiveMQ 4.x,
                            // as they have temp-queue://<destination> and temp-topic://<destination> URIs
                            // Extract to a custom resolver for ActiveMQ4.x
                            // The code path can be exercised, e.g. by a LoanBrokerESBTestCase
                            String qtype = reply.substring(0, i);
                            replyToTopic = "topic".equalsIgnoreCase(qtype);
                            reply = reply.substring(i + 1);
                        }
                        replyTo = connector.getJmsSupport().createDestination(session, reply, replyToTopic);
                    }
                }
                // Are we going to wait for a return event ?
                if (remoteSync && replyTo == null)
                {
                    replyTo = connector.getJmsSupport().createTemporaryDestination(session, topic);
                }
                // Set the replyTo property
                if (replyTo != null)
                {
                    msg.setJMSReplyTo(replyTo);
                }

                // Are we going to wait for a return event ?
                if (remoteSync)
                {
                    consumer = connector.getJmsSupport().createConsumer(session, replyTo, topic);
                }
            }

            // QoS support
            String ttlString = (String)eventMsg.removeProperty(JmsConstants.TIME_TO_LIVE_PROPERTY);
            String priorityString = (String)eventMsg.removeProperty(JmsConstants.PRIORITY_PROPERTY);
            String persistentDeliveryString = (String)eventMsg.removeProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY);

            long ttl = StringUtils.isNotBlank(ttlString)
                                ? NumberUtils.toLong(ttlString)
                                : Message.DEFAULT_TIME_TO_LIVE;
            int priority = StringUtils.isNotBlank(priorityString)
                                ? NumberUtils.toInt(priorityString)
                                : Message.DEFAULT_PRIORITY;
            boolean persistent = StringUtils.isNotBlank(persistentDeliveryString)
                                ? BooleanUtils.toBoolean(persistentDeliveryString)
                                : connector.isPersistentDelivery();

            if (connector.isHonorQosHeaders())
            {
                int priorityProp = eventMsg.getIntProperty(JmsConstants.JMS_PRIORITY, UMOConnector.INT_VALUE_NOT_SET);
                int deliveryModeProp = eventMsg.getIntProperty(JmsConstants.JMS_DELIVERY_MODE, UMOConnector.INT_VALUE_NOT_SET);
                
                if (priorityProp != UMOConnector.INT_VALUE_NOT_SET)
                {
                    priority = priorityProp;
                }
                if (deliveryModeProp != UMOConnector.INT_VALUE_NOT_SET)
                {
                    persistent = deliveryModeProp == DeliveryMode.PERSISTENT;
                }
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Sending message of type " + ClassUtils.getSimpleName(msg.getClass()));
            }

            if (consumer != null && topic)
            {
                // need to register a listener for a topic
                Latch l = new Latch();
                ReplyToListener listener = new ReplyToListener(l);
                consumer.setMessageListener(listener);

                connector.getJmsSupport().send(producer, msg, persistent, priority, ttl, topic);

                int timeout = event.getTimeout();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                }

                l.await(timeout, TimeUnit.MILLISECONDS);
                consumer.setMessageListener(null);
                listener.release();
                Message result = listener.getMessage();
                if (result == null)
                {
                    logger.debug("No message was returned via replyTo destination");
                    return null;
                }
                else
                {
                    UMOMessageAdapter adapter = connector.getMessageAdapter(result);
                    return new MuleMessage(JmsMessageUtils.toObject(result, connector.getSpecification()),
                        adapter);
                }
            }
            else
            {
                connector.getJmsSupport().send(producer, msg, persistent, priority, ttl, topic);
                if (consumer != null)
                {
                    int timeout = event.getTimeout();

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                    }

                    Message result = consumer.receive(timeout);
                    if (result == null)
                    {
                        logger.debug("No message was returned via replyTo destination");
                        return null;
                    }
                    else
                    {
                        UMOMessageAdapter adapter = connector.getMessageAdapter(result);
                        return new MuleMessage(
                            JmsMessageUtils.toObject(result, connector.getSpecification()), adapter);
                    }
                }
            }
            return null;
        }
        finally
        {
            connector.closeQuietly(producer);
            connector.closeQuietly(consumer);

            // TODO AP check if TopicResolver is to be utilized for temp destinations as well
            if (replyTo != null && (replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic))
            {
                if (replyTo instanceof TemporaryQueue)
                {
                    connector.closeQuietly((TemporaryQueue)replyTo);
                }
                else
                {
                    // hope there are no more non-standard tricks from JMS vendors
                    // here ;)
                    connector.closeQuietly((TemporaryTopic)replyTo);
                }
            }

            // If the session is from the current transaction, it is up to the
            // transaction to close it.
            if (session != null && !cached && !transacted)
            {
                connector.closeQuietly(session);
            }
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        UMOMessage message = dispatchMessage(event);
        return message;
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        Session session = null;
        MessageConsumer consumer = null;

        try
        {
            String resourceInfo = endpoint.getEndpointURI().getResourceInfo();
            boolean topic = (resourceInfo != null && JmsConstants.TOPIC_PROPERTY
                .equalsIgnoreCase(resourceInfo));

            JmsSupport support = connector.getJmsSupport();
            session = connector.getSession(false, topic);
            Destination dest = support.createDestination(session, endpoint.getEndpointURI().getAddress(),
                topic);
            consumer = support.createConsumer(session, dest, topic);

            try
            {
                Message message = null;

                if (timeout == RECEIVE_NO_WAIT)
                {
                    message = consumer.receiveNoWait();
                }
                else if (timeout == RECEIVE_WAIT_INDEFINITELY)
                {
                    message = consumer.receive();
                }
                else
                {
                    message = consumer.receive(timeout);
                }

                if (message == null)
                {
                    return null;
                }

                message = connector.preProcessMessage(message, session);

                return new MuleMessage(connector.getMessageAdapter(message));
            }
            catch (Exception e)
            {
                connector.handleException(e);
                return null;
            }
        }
        finally
        {
            connector.closeQuietly(consumer);
            connector.closeQuietly(session);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    private class ReplyToListener implements MessageListener
    {
        private final Latch latch;
        private volatile Message message;
        private final WaitableBoolean released = new WaitableBoolean(false);

        public ReplyToListener(Latch latch)
        {
            this.latch = latch;
        }

        public Message getMessage()
        {
            return message;
        }

        public void release()
        {
            released.set(true);
        }

        public void onMessage(Message message)
        {
            this.message = message;
            latch.countDown();
            try
            {
                released.whenTrue(null);
            }
            catch (InterruptedException e)
            {
                // ignored
            }
        }
    }

}
