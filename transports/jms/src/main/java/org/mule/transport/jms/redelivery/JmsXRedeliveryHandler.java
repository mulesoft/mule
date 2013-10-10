/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A redelivery handler which relies on JMS provider's redelivery count facilities.
 * @see org.mule.transport.jms.JmsConstants#JMS_X_DELIVERY_COUNT
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
                MuleMessage msg = createMuleMessage(message);
                throw new MessageRedeliveredException(messageId, redeliveryCount, connectorRedelivery, endpoint, flow, msg);
            }
        }
        else if (redeliveryCount > connectorRedelivery)
        {
            MuleMessage msg = createMuleMessage(message);
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
