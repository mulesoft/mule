/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.util.NumberUtils.toInt;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.compatibility.transport.jms.i18n.JmsMessages;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.concurrent.WaitableBoolean;

import java.util.TimerTask;
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
        dispatchMessage(event, false, null);
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

    private MuleMessage dispatchMessage(MuleEvent event, boolean doSend, final CompletionHandler<MuleMessage, Exception, Void> completionHandler) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("dispatching on endpoint: " + endpoint.getEndpointURI()
                         + ". MuleEvent id is: " + event.getId()
                         + ". Outbound transformers are: " + endpoint.getMessageProcessors());
        }

        final Message jmsMessage = getJmsMessagePayload(event);
        final MuleMessage muleRequestMessage = event.getMessage();

        boolean transacted = isTransacted();
        final boolean useReplyToDestination = isUseReplyToDestination(event, doSend, transacted);
        final boolean topic = connector.getTopicResolver().isTopic(endpoint, true);

        // QoS support
        final long ttl = muleRequestMessage.getOutboundProperty(JmsConstants.TIME_TO_LIVE_PROPERTY, Message.DEFAULT_TIME_TO_LIVE);
        int priority = muleRequestMessage.getOutboundProperty(JmsConstants.PRIORITY_PROPERTY, Message.DEFAULT_PRIORITY);
        boolean persistent = muleRequestMessage.getOutboundProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY, connector.isPersistentDelivery());

                // If we are honouring the current QoS message headers we need to use the ones set on the current message
                if (connector.isHonorQosHeaders())
                {
                    Object priorityProp = muleRequestMessage.getInboundProperty(JmsConstants.JMS_PRIORITY);
                    Object deliveryModeProp = muleRequestMessage.getInboundProperty(JmsConstants.JMS_DELIVERY_MODE);
         
            if (priorityProp != null)
            {
                priority = toInt(priorityProp);
            }
            if (deliveryModeProp != null)
            {
                persistent = toInt(deliveryModeProp) == DeliveryMode.PERSISTENT;
            }
        }

        Session session = null;
        MessageProducer producer = null;
        boolean delayedCleanup = false;

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Sending message of type " + ClassUtils.getSimpleName(jmsMessage.getClass()));
                logger.debug("Sending JMS Message type " + jmsMessage.getJMSType() +
                             "\n  JMSMessageID=" + jmsMessage.getJMSMessageID() +
                             "\n  JMSCorrelationID=" + jmsMessage.getJMSCorrelationID() +
                             "\n  JMSDeliveryMode=" + (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT) +
                             "\n  JMSPriority=" + priority +
                             "\n  JMSReplyTo=" + jmsMessage.getJMSReplyTo());
            }

            session = connector.getTransactionalResource(endpoint);
            producer = createProducer(session, topic);

            final Destination replyTo = getReplyToDestination(jmsMessage, session, event, useReplyToDestination, topic);

            // Set the replyTo property
            if (replyTo != null)
            {
                jmsMessage.setJMSReplyTo(replyTo);
            }

            jmsMessage.setStringProperty(MULE_CORRELATION_ID_PROPERTY, resolveMuleCorrelationId(event));
            jmsMessage.setJMSCorrelationID(resolveJmsCorrelationId(event));

            // Allow overrides to alter the message if necessary
            processMessage(jmsMessage, event);

            if (useReplyToDestination && replyTo != null)
            {
                final MessageConsumer consumer = createReplyToConsumer(jmsMessage, event, session, replyTo, topic);
                final int timeout = endpoint.getResponseTimeout();

                if (completionHandler != null)
                {
                    internalNonBlockingSendAndReceive(session, producer, consumer, replyTo, jmsMessage, topic, ttl, priority, persistent, transacted, timeout, completionHandler);
                    delayedCleanup = true;
                    return null;
                }
                else
                {
                    try
                    {
                                                if (topic)
                                                    {
                                                        return internalBlockingSendAndAwait(consumer, producer, replyTo, jmsMessage, topic, ttl, priority, persistent, timeout);
                                                 }

                                            else
                                            {
                                                return internalBlockingSendAndReceive(producer, consumer, replyTo, jmsMessage, topic, ttl, priority, persistent, timeout);
                                            }
                                            }
                finally
                    {
                    closeConsumer(session, consumer, replyTo);
                    }
                }
            }
            else
            {
                return internalSend(producer, jmsMessage, topic, ttl, priority, persistent);
            }
        }
        finally
        {
            if (!delayedCleanup)
            {
                connector.closeQuietly(producer);
                closeSession(session);
            }
        }
    }

    private MuleMessage internalSend(MessageProducer producer, Message jmsMessage, boolean topic, long ttl, int priority, boolean persistent) throws Exception
    {
        connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);
        return returnOriginalMessageAsReply ? createMuleMessage(jmsMessage) : null;
    }

    private MuleMessage internalBlockingSendAndAwait(MessageConsumer consumer, MessageProducer producer, Destination replyTo,
                                                     Message jmsMessage, boolean topic, long ttl, int priority, boolean persistent, int timeout)
            throws Exception
    {
        // need to register a listener for a topic
        Latch latch = new Latch();
        LatchReplyToListener listener = new LatchReplyToListener(latch);
        consumer.setMessageListener(listener);

        connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for response event for: " + timeout + " ms on " + replyTo);
        }

        latch.await(timeout, TimeUnit.MILLISECONDS);
        consumer.setMessageListener(null);
        listener.release();
        return createResponseMuleMessage(listener.getMessage(), replyTo);
    }

    private MuleMessage internalBlockingSendAndReceive(MessageProducer producer, MessageConsumer consumer, Destination replyTo, Message jmsMessage, boolean topic, long ttl, int priority,
                                                       boolean persistent, int timeout)
            throws Exception
    {
        connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for non-blocking response event for: " + timeout + " ms on " + replyTo);
        }

        Message result = consumer.receive(timeout);
        return createResponseMuleMessage(result, replyTo);
    }

    private void internalNonBlockingSendAndReceive(final Session session, final MessageProducer producer, final MessageConsumer consumer, final Destination replyTo, Message jmsMessage, boolean topic,
                                                   long ttl, int priority, boolean persistent, final boolean transacted, int timeout,
                                                   final CompletionHandler<MuleMessage, Exception, Void> completionHandler)
            throws JMSException
    {
        final TimerTask closeConsumerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    completionHandler.onCompletion(createMuleMessage(null), (ExceptionCallback<Void, Exception>) (exception ->
                    {
                        // TODO MULE-9629
                        return null;
                    }));
                }
                catch (MuleException e)
                {
                    completionHandler.onFailure(e);
                }
                finally
                {
                    cleanup(producer, session, consumer, replyTo);
                }
            }
        };
        consumer.setMessageListener(new CompletionHandlerReplyToListener(new CompletionHandler<Message, Exception, Void>()
        {
            @Override
            public void onCompletion(Message result, ExceptionCallback<Void, Exception> exceptionCallback)
            {
                try
                {
                    closeConsumerTask.cancel();
                    completionHandler.onCompletion(createResponseMuleMessage(result, replyTo), exceptionCallback);
                }
                catch (Exception e)
                {
                    completionHandler.onFailure(e);
                }
                finally
                {
                    cleanup(producer, session, consumer, replyTo);
                }
            }

            @Override
            public void onFailure(Exception exception)
            {
                try
                {
                    completionHandler.onFailure(exception);
                }
                finally
                {
                    cleanup(producer, session, consumer, replyTo);
                }
            }

        }));
        connector.getJmsSupport().send(producer, jmsMessage, persistent, priority, ttl, topic, endpoint);
        connector.scheduleTimeoutTask(closeConsumerTask, endpoint.getResponseTimeout());
    }
                                                                                 
    private void cleanup(MessageProducer producer, Session session, MessageConsumer consumer, Destination replyTo)
    {
        closeProducer(producer);
        closeConsumer(session, consumer, replyTo);
        closeSession(session);
    }

    private MessageProducer createProducer(Session session, boolean topic) throws JMSException
    {
        final Destination dest = connector.getJmsSupport().createDestination(session, endpoint);
        return connector.getJmsSupport().createProducer(session, dest, topic);
    }

    private Message getJmsMessagePayload(MuleEvent event) throws DispatchException
    {
        Object message = event.getMessage().getPayload();
        if (!(message instanceof Message))
        {
            throw new DispatchException(
                    JmsMessages.checkTransformer("JMS message", message.getClass(), connector.getName()),
                    event, getEndpoint());
        }
        return (Message) message;
    }

    private boolean isUseReplyToDestination(MuleEvent event, boolean doSend, boolean transacted)
    {
        return returnResponse(event, doSend) && !transacted;
    }

    private boolean isTransacted()
    {
        Transaction muleTx = TransactionCoordination.getInstance().getTransaction();
        return (muleTx != null && muleTx.hasResource(connector.getConnection()) || endpoint.getTransactionConfig().isTransacted());
    }

    private void closeConsumer(Session session, MessageConsumer consumer, Destination replyTo)
    {
        connector.closeQuietly(consumer);

        if (replyTo != null && (replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic))
        {
            if (replyTo instanceof TemporaryQueue)
            {
                connector.closeQuietly((TemporaryQueue) replyTo);
            }
            else
            {
                connector.closeQuietly((TemporaryTopic) replyTo);
            }
        }
    }

    private void closeProducer(MessageProducer producer)
    {
        connector.closeQuietly(producer);
    }

    private void closeSession(Session session)
    {
            // If the session is from the current transaction, it is up to the
            // transaction to close it.
        if (session != null && !isTransacted())
            {
                connector.closeQuietly(session);
            }
    }

    private MuleMessage createResponseMuleMessage(Message result, Destination replyTo) throws Exception
    {
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

    /**
     * Resolve the value of correlationID that should be used for the JMS Message. This is done here and not as part of
     * transformation because of the need for visibility of MuleEvent and OutboundEndpoint.
     *
     * @param event the current MuleEvent
     */
    protected String resolveJmsCorrelationId(MuleEvent event) throws JMSException
    {
        return resolveMuleCorrelationId(event);
    }

    /**
     * Resolve the value of Mule correlationID that should be send as a JMS header. This is done here and not as part of
     * transformation because of the need for visibility of MuleEvent and OutboundEndpoint.
     *
     * @param event the current MuleEvent
     */
    private String resolveMuleCorrelationId(MuleEvent event) throws JMSException
    {
        if (event.getFlowConstruct() != null && event.getFlowConstruct().getMessageInfoMapping() != null)
        {
            return event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(event);
        }
        else
        {
            return defaultMessageInfoMapping.getCorrelationId(event);
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
        return dispatchMessage(event, true, null);
    }

    @Override
    protected void doSendNonBlocking(MuleEvent event, CompletionHandler<MuleMessage, Exception, Void> completionHandler)
    {
        try
        {
            dispatchMessage(event, true, completionHandler);
        }
        catch (Exception e)
        {
            completionHandler.onFailure(e);
        }
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

    protected MessageConsumer createReplyToConsumer(Message jmsMessage, MuleEvent event,
                                                    Session session, Destination replyTo, boolean topic) throws JMSException
    {
        String selector = null;
        //Only used by topics
        String durableName;
        //If we're not using
        if (!(replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic))
        {
            selector = "JMSCorrelationID='" + jmsMessage.getJMSCorrelationID() + "'";
            if (logger.isDebugEnabled())
            {
                logger.debug("ReplyTo Selector is: " + selector);
            }
        }

        //We need to set the durableName and Selector if using topics
        if (topic)
        {
            String tempDurable = event.getFlowVariable(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable);
            }
            // Get the durable subscriber name if there is one
            durableName = (String) event.getFlowVariable(JmsConstants.DURABLE_NAME_PROPERTY);
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
                        tempReplyTo = tempReplyTo.toString().substring(i + 3);
                    }
                    else
                    {
                        EndpointBuilder epb = lookupEndpointBuilder(event.getMuleContext().getRegistry(), tempReplyTo.toString());
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
                        // The code path can be exercised, e.g. by a LoanBrokerMuleTestCase
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

    protected class LatchReplyToListener implements MessageListener
    {
        private final Latch latch;
        private volatile Message message;
        private final WaitableBoolean released = new WaitableBoolean(false);

        public LatchReplyToListener(Latch latch)
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

        @Override
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
    
    private class CompletionHandlerReplyToListener implements MessageListener
    {
        private final CompletionHandler<Message, Exception, Void> completionHandler;

        public CompletionHandlerReplyToListener(CompletionHandler<Message, Exception, Void> completionHandler)
        {
            this.completionHandler = completionHandler;
        }

        @Override
        public void onMessage(Message message)
        {
            completionHandler.onCompletion(message, (ExceptionCallback<Void, Exception>) (exception ->
            {
                // TODO MULE-9629
                return null;
            }));
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

    @Override
    protected boolean isSupportsNonBlocking()
    {
        return true;
    }
}
