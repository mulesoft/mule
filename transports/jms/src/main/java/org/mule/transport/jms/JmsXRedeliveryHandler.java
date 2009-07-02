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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.jms.i18n.JmsMessages;

import java.text.MessageFormat;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A redelivery handler which relies on JMS provider's redelivery count facilities.
 * @see org.mule.transport.jms.JmsConstants#JMS_X_DELIVERY_COUNT
 */
public class JmsXRedeliveryHandler implements RedeliveryHandler
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(JmsXRedeliveryHandler.class);

    protected JmsConnector connector;

    /**
     * The connector associated with this handler is set before
     * <code>handleRedelivery()</code> is called
     *
     * @param connector the connector associated with this handler
     */
    public void setConnector(JmsConnector connector)
    {
        this.connector = connector;
    }

    /**
     * process the redelivered message. If the Jms receiver should process the
     * message, it should be returned. Otherwise the connector should throw a
     * <code>MessageRedeliveredException</code> to indicate that the message should
     * be handled by the connector Exception Handler.
     * 
     */
    public void handleRedelivery(Message message) throws JMSException, MuleException
    {
        if (connector.getMaxRedelivery() <= 0)
        {
            return;
        }

        String messageId = message.getJMSMessageID();
        int deliveryCount = -1;
        try
        {
            deliveryCount = message.getIntProperty(JmsConstants.JMS_X_DELIVERY_COUNT);
        }
        catch (NumberFormatException nex)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage(String.format(
                    "Invalid use of %s. Message is flagged with JMSRedelivered, but JMSXDeliveryCount is not set",
                    getClass().getName())));
        }

        int redeliveryCount = deliveryCount - 1;

        if (redeliveryCount == 1)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message with id: " + messageId + " has been redelivered for the first time");
            }
        }
        else if (redeliveryCount > connector.getMaxRedelivery())
        {
            logger.debug(MessageFormat.format(
                    "Message with id: {0} has been redelivered {1} times, which exceeds the maxRedelivery setting " +
                    "of {2} on the connector {3}", messageId, redeliveryCount, connector.getMaxRedelivery(), connector.getName()));

            JmsMessageAdapter adapter = (JmsMessageAdapter) connector.getMessageAdapter(message);
            throw new MessageRedeliveredException(
                JmsMessages.tooManyRedeliveries(messageId, String.valueOf(redeliveryCount),
                                                connector.getMaxRedelivery(), connector.getName()), new DefaultMuleMessage(adapter, connector.getMuleContext()));

        }
        else
        {
            if (logger.isDebugEnabled())
            {
                // re-delivery count is actually less by 1 than an actual delivery count
                logger.debug("Message with id: " + messageId + " has been redelivered " + redeliveryCount + " times");
            }
        }
    }
}