/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.impl.model.AbstractComponent;
import org.mule.providers.DefaultReplyToHandler;
import org.mule.providers.jms.i18n.JmsMessages;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.StringMessageUtils;

import java.util.Iterator;
import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * <code>JmsReplyToHandler</code> will process a JMS replyTo or hand off to the
 * default replyTo handler if the replyTo is a URL
 */
public class JmsReplyToHandler extends DefaultReplyToHandler
{
    private final JmsConnector connector;

    public JmsReplyToHandler(JmsConnector connector, List transformers)
    {
        super(transformers);
        this.connector = connector;
    }

    public void processReplyTo(UMOEvent event, UMOMessage returnMessage, Object replyTo) throws UMOException
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
            //to be set on the transformer so that a JMSMEssage can be created correctly
            Class srcType = returnMessage.getPayload().getClass();
            for (Iterator iterator = getTransformers().iterator(); iterator.hasNext();)
            {
                UMOTransformer t = (UMOTransformer)iterator.next();
                if(t.isSourceTypeSupported(srcType))
                {
                    if(t.getEndpoint()==null)
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

            replyToMessage.setJMSReplyTo(null);
            if (logger.isDebugEnabled())
            {
                logger.debug("Sending jms reply to: " + replyToDestination + "("
                             + replyToDestination.getClass().getName() + ")");
            }
            replyToProducer = connector.getJmsSupport().createProducer(session, replyToDestination, topic);

            // QoS support
            UMOMessage eventMsg = event.getMessage();
            String ttlString = (String)eventMsg.removeProperty(JmsConstants.TIME_TO_LIVE_PROPERTY);
            String priorityString = (String)eventMsg.removeProperty(JmsConstants.PRIORITY_PROPERTY);
            String persistentDeliveryString = (String)eventMsg.removeProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY);

            if (ttlString == null && priorityString == null && persistentDeliveryString == null)
            {
                connector.getJmsSupport().send(replyToProducer, replyToMessage, topic);
            }
            else
            {
                long ttl = Message.DEFAULT_TIME_TO_LIVE;
                int priority = Message.DEFAULT_PRIORITY;

                // TODO this first assignment is ignored anyway, review and remove if need to
                boolean persistent = Message.DEFAULT_DELIVERY_MODE == DeliveryMode.PERSISTENT;

                if (ttlString != null)
                {
                    ttl = Long.parseLong(ttlString);
                }
                if (priorityString != null)
                {
                    priority = Integer.parseInt(priorityString);
                }
                // TODO StringUtils.notBlank() would be more robust here
                persistent = persistentDeliveryString != null
                                ? Boolean.valueOf(persistentDeliveryString).booleanValue()
                                : connector.isPersistentDelivery();

                connector.getJmsSupport().send(replyToProducer, replyToMessage, persistent, priority, ttl,
                    topic);
            }

            // connector.getJmsSupport().send(replyToProducer, replyToMessage,
            // replyToDestination);
            logger.info("Reply Message sent to: " + replyToDestination);
            ((AbstractComponent)event.getComponent()).getStatistics().incSentReplyToEvent();
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
}
