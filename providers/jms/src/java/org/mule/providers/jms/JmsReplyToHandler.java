/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms;

import org.mule.impl.MuleComponent;
import org.mule.providers.DefaultReplyToHandler;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.UMOTransformer;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * <code>JmsReplyToHandler</code> will process a Jms replyTo or hand off
 * to the defualt replyTo handler if the replyTo is a url
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsReplyToHandler extends DefaultReplyToHandler implements Disposable
{
    private JmsConnector connector;
    private Session session;
    private MessageProducer replyToProducer;

    public JmsReplyToHandler(JmsConnector connector, Session session, UMOTransformer transformer)
    {
        super(transformer);
        this.connector = connector;
        this.session = session;
    }

    public void processReplyTo(UMOEvent event, UMOMessage returnMessage, Object replyTo) throws UMOException
    {
        Destination replyToDestination = null;
        try
        {
            // now we need to send the response
            if (replyTo instanceof Destination)
            {
                replyToDestination = (Destination) replyTo;
            }
            if (replyToDestination == null)
            {
                super.processReplyTo(event, returnMessage, replyTo);
                return;
            }
            Object payload = returnMessage.getPayload();
            if (getTransformer() != null)
            {
                payload = getTransformer().transform(payload);
            }
            Message replyToMessage = JmsMessageUtils.getMessageForObject(payload, session);

            if (logger.isDebugEnabled())
            {
                logger.debug("Sending jms reply to: " + replyToDestination + "(" + replyToDestination.getClass().getName() + ")");
            }
            if (replyToProducer == null)
            {
                replyToProducer = ((JmsConnector) connector).getJmsSupport().createProducer(session, replyToDestination);
            }

            //QoS support
            String ttlString = (String) event.removeProperty("TimeToLive");
            String priorityString = (String) event.removeProperty("Priority");
            String persistentDeliveryString = (String) event.removeProperty("PersistentDelivery");

            if (ttlString == null && priorityString == null && persistentDeliveryString == null)
            {
                connector.getJmsSupport().send(replyToProducer, replyToMessage);
            } else
            {
                long ttl = Message.DEFAULT_TIME_TO_LIVE;
                int priority = Message.DEFAULT_PRIORITY;
                boolean persistent = Message.DEFAULT_DELIVERY_MODE == DeliveryMode.PERSISTENT;

                if (ttlString != null) ttl = Long.parseLong(ttlString);
                if (priorityString != null) priority = Integer.parseInt(priorityString);
                if (persistentDeliveryString != null) persistent = Boolean.valueOf(persistentDeliveryString).booleanValue();

                connector.getJmsSupport().send(replyToProducer, replyToMessage, persistent, priority, ttl);
            }

            //connector.getJmsSupport().send(replyToProducer, replyToMessage, replyToDestination);
            logger.info("Reply Message sent to: " + replyToDestination);
            ((MuleComponent) event.getComponent()).getStatistics().incSentReplyToEvent();
        } catch (Exception e)
        {
            throw new DispatchException(new org.mule.config.i18n.Message("jms", 8, replyToDestination), returnMessage, null);
        } finally
        {
            dispose();
        }
    }

    public void dispose()
    {
        try
        {
            if (replyToProducer != null) replyToProducer.close();
        } catch (JMSException e)
        {
            logger.error("Failed to close replyTo producer: " + e.getMessage(), e);
        }
    }
}
