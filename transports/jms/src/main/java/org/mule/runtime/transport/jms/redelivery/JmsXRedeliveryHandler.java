/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.JmsConstants;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A redelivery handler which relies on JMS provider's redelivery count facilities.
 * @see org.mule.runtime.transport.jms.JmsConstants#JMS_X_DELIVERY_COUNT
 */
public class JmsXRedeliveryHandler extends AbstractRedeliveryHandler
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(JmsXRedeliveryHandler.class);

    /**
     * process the redelivered message. If the Jms receiver should process the
     * message, it should be returned. Otherwise the connector should throw a
     * <code>MessageRedeliveredException</code> to indicate that the message should
     * be handled by the connector Exception Handler.
     * 
     */
    @Override
    public void handleRedelivery(Message message, InboundEndpoint endpoint, FlowConstruct flow) throws JMSException, MuleException
    {
        final int connectorRedelivery = connector.getMaxRedelivery();
        if (connectorRedelivery == JmsConnector.REDELIVERY_IGNORE || connectorRedelivery < 0 ) // just in case, for manual setting)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("We were asked to ignore the redelivery count, nothing to do here.");
            }
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

            if (connectorRedelivery == JmsConnector.REDELIVERY_FAIL_ON_FIRST)
            {
                MuleMessage msg = createMuleMessage(message, endpoint.getMuleContext());
                throw new MessageRedeliveredException(messageId, redeliveryCount, connectorRedelivery, endpoint, flow, msg);
            }
        }
        else if (redeliveryCount > connectorRedelivery)
        {
            MuleMessage msg = createMuleMessage(message, endpoint.getMuleContext());
            throw new MessageRedeliveredException(messageId, redeliveryCount, connectorRedelivery, endpoint, flow, msg);
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
