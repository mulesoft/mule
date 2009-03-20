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

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.DispatchException;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.transport.jms.i18n.JmsMessages;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

import java.util.Iterator;
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
    private final JmsConnector connector;

    public JmsReplyToHandler(JmsConnector connector, List transformers)
    {
        super(transformers);
        this.connector = connector;
    }

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

            //This is a work around for JmsTransformers where the current endpoint needs
            //to be set on the transformer so that a JMSMessage can be created correctly (the transformer needs a Session)
            Class srcType = returnMessage.getPayload().getClass();
                for (Iterator iterator = getTransformers().iterator(); iterator.hasNext();)
            {
                Transformer t = (Transformer) iterator.next();
                if (t.isSourceTypeSupported(srcType))
                {
                    if (t.getEndpoint() == null)
                    {
                        t.setEndpoint(getEndpoint(event, "jms://temporary"));
                        break;
                    }
                }
            }
            returnMessage.applyTransformers(getTransformers());
            Object payload = returnMessage.getPayload();

            if (replyToDestination instanceof Topic && replyToDestination instanceof Queue
                    && connector.getJmsSupport() instanceof Jms102bSupport)
            {
                logger.error(StringMessageUtils.getBoilerPlate("ReplyTo destination implements both Queue and Topic "
                                                               + "while complying with JMS 1.0.2b specification. "
                                                               + "Please report your application server or JMS vendor name and version "
                                                               + "to dev<_at_>mule.codehaus.org or http://mule.mulesource.org/jira"));
            }

            final boolean topic = connector.getTopicResolver().isTopic(replyToDestination);
            session = connector.getSession(false, topic);
            Message replyToMessage = JmsMessageUtils.toMessage(payload, session);

            processMessage(replyToMessage, event);
            if (logger.isDebugEnabled())
            {
                logger.debug("Sending jms reply to: " + replyToDestination + "("
                             + replyToDestination.getClass().getName() + ")");
            }
            replyToProducer = connector.getJmsSupport().createProducer(session, replyToDestination, topic);

            // QoS support
            MuleMessage eventMsg = event.getMessage();
            String ttlString = (String)eventMsg.removeProperty(JmsConstants.TIME_TO_LIVE_PROPERTY);
            String priorityString = (String)eventMsg.removeProperty(JmsConstants.PRIORITY_PROPERTY);
            String persistentDeliveryString = (String)eventMsg.removeProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY);

            String correlationIDString = replyToMessage.getJMSCorrelationID();
            if (StringUtils.isBlank(correlationIDString))
            {
                correlationIDString = (String) eventMsg.getProperty(JmsConstants.JMS_MESSAGE_ID);
                replyToMessage.setJMSCorrelationID(correlationIDString);
            }

            if (ttlString == null && priorityString == null && persistentDeliveryString == null)
            {
                connector.getJmsSupport().send(replyToProducer, replyToMessage, topic);
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
                                ? Boolean.valueOf(persistentDeliveryString).booleanValue()
                                : connector.isPersistentDelivery();

                connector.getJmsSupport().send(replyToProducer, replyToMessage, persistent, priority, ttl,
                    topic);
            }

            logger.info("Reply Message sent to: " + replyToDestination +" with correlationID:" + correlationIDString);
            event.getService().getStatistics().incSentReplyToEvent();
        }
        catch (Exception e)
        {
            throw new DispatchException(
                JmsMessages.failedToCreateAndDispatchResponse(replyToDestination), returnMessage, null, e);
        }
        finally
        {
            connector.closeQuietly(replyToProducer);
            connector.closeQuietly(session);
        }
    }

    protected void processMessage(Message replyToMessage, MuleEvent event) throws JMSException
    {
        replyToMessage.setJMSReplyTo(null);

        // If JMS correlation ID exists in the incoming message - use it for the outbound message;
        // otherwise use JMS Message ID
        MuleMessage eventMsg = event.getMessage();
        Object jmsCorrelationId = eventMsg.getProperty("JMSCorrelationID");
        if (jmsCorrelationId == null)
        {
            jmsCorrelationId = eventMsg.getProperty("JMSMessageID");
}
        if (jmsCorrelationId != null)
        {
            replyToMessage.setJMSCorrelationID(jmsCorrelationId.toString());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("replyTomessage is " + replyToMessage);
        }
    }
}
