/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.connector.DefaultReplyToHandler;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.transport.jms.i18n.JmsMessages;
import org.mule.runtime.transport.jms.transformers.ObjectToJMSMessage;

import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * <code>JmsReplyToHandler</code> will process a JMS replyTo or hand off to the
 * default replyTo handler if the replyTo is a URL.
 * The purpose of this class is to send a result on a ReplyTo destination if one
 * has been set.
 * Note that the {@link JmsMessageDispatcher} also contains logic for handling ReplyTo. However,
 * the dispatcher is responsible attaching the replyTo information to the message and also
 * receiving on the same replyTo if 'remoteSync' is set. The {@link JmsMessageDispatcher} never
 * writes to the 'replyTo' destination.
 */
public class JmsReplyToHandler extends DefaultReplyToHandler
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private transient JmsConnector jmsConnector;
    private transient ObjectToJMSMessage toJmsMessage;

    public JmsReplyToHandler(JmsConnector connector, MuleContext muleContext)
    {
        super(muleContext);
        this.connector = this.jmsConnector = connector;
        toJmsMessage = new ObjectToJMSMessage();
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        Destination replyToDestination = null;  
        MessageProducer replyToProducer = null;
        Session session = null;
        try
        {
            // now we need to send the response
            if (replyTo instanceof Destination)
            {
                replyToDestination = (Destination)replyTo;
            }
            if (replyToDestination == null)
            {
                super.processReplyTo(event, returnMessage, replyTo);
                return;
            }

            Class srcType = returnMessage.getPayload().getClass();

            EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(String.format("%s://temporary",connector.getProtocol()), muleContext);
            endpointBuilder.setConnector(jmsConnector);
            OutboundEndpoint tempEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
            
            List<Transformer> defaultTransportTransformers = ((org.mule.runtime.core.transport.AbstractConnector) jmsConnector).getDefaultOutboundTransformers(tempEndpoint);
            
            returnMessage = muleContext.getTransformationService().applyTransformers(returnMessage, null, defaultTransportTransformers);
            Object payload = returnMessage.getPayload();

            if (replyToDestination instanceof Topic && replyToDestination instanceof Queue
                    && jmsConnector.getJmsSupport() instanceof Jms102bSupport)
            {
                logger.error(StringMessageUtils.getBoilerPlate("ReplyTo destination implements both Queue and Topic "
                                                               + "while complying with JMS 1.0.2b specification. "
                                                               + "Please report your application server or JMS vendor name and version "
                                                               + "to dev<_at_>mule.codehaus.org or http://mule.mulesoft.org/jira"));
            }

            final boolean topic = jmsConnector.getTopicResolver().isTopic(replyToDestination);
            session = jmsConnector.getSession(false, topic);

            //This mimics the OBjectToJmsMessage Transformer behaviour without needing an endpoint
            //TODO clean this up, maybe make the transformer available via a utility class, passing in the Session
            Message replyToMessage = JmsMessageUtils.toMessage(payload, session);
            connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), returnMessage);
            toJmsMessage.setJmsProperties(returnMessage, replyToMessage);

            processMessage(replyToMessage, event);
            if (logger.isDebugEnabled())
            {
                logger.debug("Sending jms reply to: " + replyToDestination + " ("
                             + replyToDestination.getClass().getName() + ")");
            }
            replyToProducer = jmsConnector.getJmsSupport().createProducer(session, replyToDestination, topic);

            // QoS support
            MuleMessage eventMsg = event.getMessage();
            String ttlString = (String)eventMsg.getOutboundProperty(JmsConstants.TIME_TO_LIVE_PROPERTY);
            String priorityString = (String)eventMsg.getOutboundProperty(JmsConstants.PRIORITY_PROPERTY);
            String persistentDeliveryString = (String)eventMsg.getOutboundProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY);

            String correlationIDString = replyToMessage.getJMSCorrelationID();
            if (StringUtils.isBlank(correlationIDString))
            {
                correlationIDString = eventMsg.getInboundProperty(JmsConstants.JMS_MESSAGE_ID);
                replyToMessage.setJMSCorrelationID(correlationIDString);
            }

            if (ttlString == null && priorityString == null && persistentDeliveryString == null)
            {
                jmsConnector.getJmsSupport().send(replyToProducer, replyToMessage, topic, null);
            }
            else
            {
                long ttl = Message.DEFAULT_TIME_TO_LIVE;
                int priority = Message.DEFAULT_PRIORITY;

                if (ttlString != null)
                {
                    ttl = Long.parseLong(ttlString);
                }
                if (priorityString != null)
                {
                    priority = Integer.parseInt(priorityString);
                }
                boolean persistent = StringUtils.isNotBlank(persistentDeliveryString)
                                ? Boolean.valueOf(persistentDeliveryString)
                                : jmsConnector.isPersistentDelivery();

                jmsConnector.getJmsSupport().send(replyToProducer, replyToMessage, persistent, priority, ttl,
                    topic, null);
            }

            if (logger.isInfoEnabled())
            {
                logger.info(String.format("Reply Message sent to: %s with correlationID:%s", replyToDestination, correlationIDString));
            }
        }
        catch (Exception e)
        {
            throw new DispatchException(
                JmsMessages.failedToCreateAndDispatchResponse(replyToDestination), event, null, e);
        }
        finally
        {
            jmsConnector.closeQuietly(replyToProducer);
            jmsConnector.closeSessionIfNoTransactionActive(session);
        }
    }

    protected void processMessage(Message replyToMessage, MuleEvent event) throws JMSException
    {
        replyToMessage.setJMSReplyTo(null);

        // If JMS correlation ID exists in the incoming message - use it for the outbound message;
        // otherwise use JMS Message ID
        MuleMessage eventMsg = event.getMessage();
        String jmsCorrelationId = eventMsg.getInboundProperty("JMSCorrelationID");
        if (jmsCorrelationId == null)
        {
            jmsCorrelationId = eventMsg.getInboundProperty("JMSMessageID");
        }
        if (jmsCorrelationId != null)
        {
            replyToMessage.setJMSCorrelationID(jmsCorrelationId);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("replyTo message is " + replyToMessage);
        }
    }

    @Override
    public void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        super.initAfterDeserialisation(muleContext);
        this.toJmsMessage = new ObjectToJMSMessage();
        this.jmsConnector = (JmsConnector) this.connector;
    }
}
