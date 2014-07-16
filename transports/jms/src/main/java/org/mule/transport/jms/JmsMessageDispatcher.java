/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.jms.i18n.JmsMessages;
import org.mule.util.ClassUtils;
import org.mule.util.NumberUtils;
import org.mule.util.concurrent.Latch;
import org.mule.util.concurrent.WaitableBoolean;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages to JMS
 * destinations. All JMS semantics apply and settings such as replyTo and QoS
 * properties are read from the event properties or defaults are used (according to
 * the JMS specification)
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher
{

    private JmsConnector connector;
    private boolean disableTemporaryDestinations = false;
    private boolean returnOriginalMessageAsReply = false;

    public JmsMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JmsConnector) endpoint.getConnector();
        disableTemporaryDestinations = connector.isDisableTemporaryReplyToDestinations() ||
            ("true".equals(endpoint.getProperty(JmsConstants.DISABLE_TEMP_DESTINATIONS_PROPERTY)));
        returnOriginalMessageAsReply = connector.isReturnOriginalMessageAsReply() ||
            ("true".equals(endpoint.getProperty(JmsConstants.RETURN_ORIGINAL_MESSAGE_PROPERTY)));
        if (returnOriginalMessageAsReply && !disableTemporaryDestinations)
        {
            logger.warn("The returnOriginalMessageAsReply property will be ignored because disableTemporaryReplyToDestinations=false.  You need to disable temporary ReplyTo destinations in order for this propery to take effect.");
        }
        logger.warn("Starting patched JmsMessageReceiver");
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (connector.getConnection() == null)
        {
            throw new IllegalStateException("No JMS Connection");
        }
        dispatchMessage(event, false);
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected boolean isDisableTemporaryDestinations()
    {
        return disableTemporaryDestinations;
    }

    private MuleMessage dispatchMessage(MuleEvent event, boolean doSend) throws Exception
    {
        Session session = null;
        MessageProducer producer = null;
        MessageConsumer consumer = null;
        Destination replyTo = null;
        boolean transacted = false;
        boolean cached = false;
        boolean useReplyToDestination;

        final Transaction muleTx = TransactionCoordination.getInstance().getTransaction();

        if (logger.isDebugEnabled())
        {
            logger.debug("dispatching on endpoint: " + endpoint.getEndpointURI()
                    + ". MuleEvent id is: " + event.getId()
                    + ". Outbound transformers are: " + endpoint.getTransformers());
        }

        try
        {
            session = connector.getTransactionalResource(endpoint);

            transacted = (muleTx != null && muleTx.hasResource(connector.getConnection()) || endpoint.getTransactionConfig().isTransacted());

            // If a transaction is running, we can not receive any messages
            // in the same transaction using a replyTo destination
            useReplyToDestination = returnResponse(event, doSend) && !transacted;

            boolean topic = connector.getTopicResolver().isTopic(endpoint, true);

            Destination dest = connector.getJmsSupport().createDestination(session, endpoint);
            producer = connector.getJmsSupport().createProducer(session, dest, topic);

            Object message = event.getMessage().getPayload();
            if (!(message instanceof Message))
            {
                throw new DispatchException(
                        JmsMessages.checkTransformer("JMS message", message.getClass(), connector.getName()),
                        event, getEndpoint());
            }

            Message msg = (Message) message;

            MuleMessage eventMsg = event.getMessage();

            replyTo = getReplyToDestination(msg, session, event, useReplyToDestination, topic);

            // Set the replyTo property
            if (replyTo != null)
            {
                msg.setJMSReplyTo(replyTo);
            }

            //Allow overrides to alter the message if necessary
            processMessage(msg, event);

            // QoS support
            long ttl = eventMsg.getOutboundProperty(JmsConstants.TIME_TO_LIVE_PROPERTY, Message.DEFAULT_TIME_TO_LIVE);
            int priority = eventMsg.getOutboundProperty(JmsConstants.PRIORITY_PROPERTY, Message.DEFAULT_PRIORITY);
            boolean persistent= eventMsg.getOutboundProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY, connector.isPersistentDelivery());

            // If we are honouring the current QoS message headers we need to use the ones set on the current message
            if (connector.isHonorQosHeaders())
            {
                Object priorityProp = eventMsg.getInboundProperty(JmsConstants.JMS_PRIORITY);
                Object deliveryModeProp = eventMsg.getInboundProperty(JmsConstants.JMS_DELIVERY_MODE);

                if (priorityProp != null)
                {
                    priority = NumberUtils.toInt(priorityProp);
                }
                if (deliveryModeProp != null)
                {
                    persistent = NumberUtils.toInt(deliveryModeProp) == DeliveryMode.PERSISTENT;
                }
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Sending message of type " + ClassUtils.getSimpleName(msg.getClass()));
                logger.debug("Sending JMS Message type " + msg.getJMSType() +
                       "\n  JMSMessageID=" + msg.getJMSMessageID() +
                       "\n  JMSCorrelationID=" + msg.getJMSCorrelationID() +
                       "\n  JMSDeliveryMode=" + (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT) +
                       "\n  JMSPriority=" + priority +
                       "\n  JMSReplyTo=" + msg.getJMSReplyTo());
            }
            connector.getJmsSupport().send(producer, msg, persistent, priority, ttl, topic, endpoint);

            if (useReplyToDestination && replyTo != null)
            {
                consumer = createReplyToConsumer(msg, event, session, replyTo, topic);

                if (topic)
                {
                    // need to register a listener for a topic
                    Latch l = new Latch();
                    ReplyToListener listener = new ReplyToListener(l);
                    consumer.setMessageListener(listener);

                    connector.getJmsSupport().send(producer, msg, persistent, priority, ttl, topic, endpoint);

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
                        return createNullMuleMessage();
                    }
                    else
                    {
                        return createMessageWithJmsMessagePayload(result);
                    }
                }
                else
                {
                    int timeout = event.getTimeout();

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                    }

                    Message result = consumer.receive(timeout);
                    if (result == null)
                    {
                        logger.debug("No message was returned via replyTo destination " + replyTo);
                        return createNullMuleMessage();
                    }
                    else
                    {
                        return createMessageWithJmsMessagePayload(result);
                    }
                }
            }
            else
            {
                // In this case a response was never expected so we return null and not NullPayload.
                // This generally happens when dispatch is used for an asynchronous endpoint but can also occur when send() is used 
                // and disableTempDestinations is set.
                return returnOriginalMessageAsReply ? createMuleMessage(msg) : null;
            }
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
                    connector.closeQuietly((TemporaryQueue) replyTo);
                }
                else
                {
                    // hope there are no more non-standard tricks from JMS vendors
                    // here ;)
                    connector.closeQuietly((TemporaryTopic) replyTo);
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

    protected MuleMessage createMessageWithJmsMessagePayload(Message jmsMessage) throws Exception
    {
        MuleMessage muleMessage = createMuleMessage(jmsMessage);
        Object payload = JmsMessageUtils.toObject(jmsMessage, connector.getSpecification(),
            endpoint.getEncoding());
        muleMessage.setPayload(payload);
        return muleMessage;
    }
    
    /**
     * This method is called before the current message is transformed.  It can be used to do any message body or
     * header processing before the transformer is called.
     *
     * @param message the current MuleMessage Being processed
     * @throws Exception
     */
    protected void preTransformMessage(MuleMessage message) throws Exception
    {
        // nothing to do
    }

    @Deprecated
    protected void handleMultiTx(Session session) throws Exception
    {
        logger.debug("Multi-transaction support is not available in Mule Community Edition.");
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        return dispatchMessage(event, true);
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    /**
     * This method is called once the JMS message is created.  It allows subclasses to alter the
     * message if necessary.
     *
     * @param msg   The JMS message that will be sent
     * @param event the current event
     * @throws JMSException if the JmsMessage cannot be written to, this should not happen because 
     *          the JMSMessage passed in will always be newly created
     */
    protected void processMessage(Message msg, MuleEvent event) throws JMSException
    {
        // template Method
    }

    /**
     * Some JMS implementations do not support ReplyTo or require some further fiddling of the message
     *
     * @param msg   The JMS message that will be sent
     * @param event the current event
     * @return true if this request should honour any JMSReplyTo settings on the message
     * @throws JMSException if the JmsMessage cannot be written to, this should not happen because the JMSMessage passed
     *                      in will always be newly created
     */
    protected boolean isHandleReplyTo(Message msg, MuleEvent event) throws JMSException
    {
        return connector.supportsProperty(JmsConstants.JMS_REPLY_TO);
    }

    protected MessageConsumer createReplyToConsumer(Message currentMessage, MuleEvent event,
                                                    Session session, Destination replyTo, boolean topic) throws JMSException
    {
        String selector = null;
        //Only used by topics
        String durableName;
        //If we're not using
        if (!(replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic))
        {
            String jmsCorrelationId = currentMessage.getJMSCorrelationID();
            if (jmsCorrelationId == null)
            {
                jmsCorrelationId = currentMessage.getJMSMessageID();
            }

            selector = "JMSCorrelationID='" + jmsCorrelationId + "'";
            if (logger.isDebugEnabled())
            {
                logger.debug("ReplyTo Selector is: " + selector);
            }
        }

        //We need to set the durableName and Selector if using topics
        if (topic)
        {
            String tempDurable = event.getMessage().getInvocationProperty(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable);
            }
            // Get the durable subscriber name if there is one
            durableName = (String) event.getMessage().getInvocationProperty(
                JmsConstants.DURABLE_NAME_PROPERTY);
            if (durableName == null && durable && topic)
            {
                durableName = "mule." + connector.getName() + "." + event.getMessageSourceURI();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: " +
                                 durableName);
                }
            }
        }
        return connector.getJmsSupport().createConsumer(session, replyTo, selector,
                                                        connector.isNoLocal(), null, topic, endpoint);
    }

    protected Destination getReplyToDestination(Message message, Session session, MuleEvent event, boolean remoteSync, boolean topic) throws JMSException, EndpointException, InitialisationException
    {
        Destination replyTo = null;

        // Some JMS implementations might not support the ReplyTo property.
        if (isHandleReplyTo(message, event))
        {

            Object tempReplyTo = event.getMessage().getOutboundProperty(JmsConstants.JMS_REPLY_TO);
            if (tempReplyTo == null)
            {
                //It may be a Mule URI or global endpoint Ref
                tempReplyTo = event.getMessage().getOutboundProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
                if (tempReplyTo != null)
                {
                    int i = tempReplyTo.toString().indexOf("://");
                    if (i > -1)
                    {
                        tempReplyTo = tempReplyTo.toString().substring(i+3);
                    }
                    else
                    {
                        EndpointBuilder epb = event.getMuleContext().getRegistry().lookupEndpointBuilder(tempReplyTo.toString());
                        if (epb != null)
                        {
                            tempReplyTo = epb.buildOutboundEndpoint().getEndpointURI().getAddress();
                        }
                    }
                }
            }
            if (tempReplyTo != null)
            {
                if (tempReplyTo instanceof Destination)
                {
                    replyTo = (Destination) tempReplyTo;
                }
                else
                {
                    // TODO AP should this drill-down be moved into the resolver as well?
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
                        replyToTopic = JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(qtype);
                        reply = reply.substring(i + 1);
                    }
                    replyTo = connector.getJmsSupport().createDestination(session, reply, replyToTopic, endpoint);
                }
            }
            // Are we going to wait for a return event ?
            if (remoteSync && replyTo == null && !disableTemporaryDestinations)
            {
                replyTo = connector.getJmsSupport().createTemporaryDestination(session, topic);
            }
        }
        return replyTo;

    }

    protected class ReplyToListener implements MessageListener
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
    
    @Override
    protected void applyOutboundTransformers(MuleEvent event) throws MuleException
    {
        try
        {
            preTransformMessage(event.getMessage());
        }
        catch (Exception e)
        {
            throw new TransformerException(CoreMessages.failedToInvoke("preTransformMessage"), e);
        }
        super.applyOutboundTransformers(event);
    }

}
