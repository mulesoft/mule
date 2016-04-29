/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.transport.AbstractMuleMessageFactory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

public class JmsMuleMessageFactory extends AbstractMuleMessageFactory
{

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{ Message.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(DefaultMuleMessage muleMessage, Object transportMessage) throws Exception
    {        
        Message jmsMessage = (Message) transportMessage;
        
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        addCorrelationProperties(jmsMessage, muleMessage, messageProperties);
        addDeliveryModeProperty(jmsMessage, messageProperties);
        addDestinationProperty(jmsMessage, messageProperties);
        addExpirationProperty(jmsMessage, messageProperties);
        addMessageIdProperty(jmsMessage, messageProperties);
        addPriorityProperty(jmsMessage, messageProperties);
        addRedeliveredProperty(jmsMessage, messageProperties);
        addJMSReplyTo(muleMessage, jmsMessage);
        addTimestampProperty(jmsMessage, messageProperties);
        addTypeProperty(jmsMessage, messageProperties);

        propagateJMSProperties(jmsMessage, messageProperties);
        
        muleMessage.addInboundProperties(messageProperties);
    }

    protected void propagateJMSProperties(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            Enumeration<?> e = jmsMessage.getPropertyNames();
            while (e.hasMoreElements())
            {
                String key = (String) e.nextElement();
                try
                {
                    Object value = jmsMessage.getObjectProperty(key);
                    if (value != null)
                    {
                        messageProperties.put(key, value);
                    }
                }
                catch (JMSException e1)
                {
                    // ignored
                }
            }
        }
        catch (JMSException e1)
        {
            // ignored
        }
    }

    protected void addTypeProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            String value = jmsMessage.getJMSType();
            if (value != null)
            {
                messageProperties.put(JmsConstants.JMS_TYPE, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addTimestampProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            long value = jmsMessage.getJMSTimestamp();
            messageProperties.put(JmsConstants.JMS_TIMESTAMP, Long.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addJMSReplyTo(MuleMessage muleMessage, Message jmsMessage)
    {
        try
        {
            Destination replyTo = jmsMessage.getJMSReplyTo();
            if (replyTo != null)
            {
                muleMessage.setOutboundProperty(JmsConstants.JMS_REPLY_TO, replyTo);
            }

            muleMessage.setReplyTo(replyTo);
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addRedeliveredProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            boolean value = jmsMessage.getJMSRedelivered();
            messageProperties.put(JmsConstants.JMS_REDELIVERED, Boolean.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addPriorityProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            int value = jmsMessage.getJMSPriority();
            messageProperties.put(JmsConstants.JMS_PRIORITY, Integer.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addMessageIdProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            String value = jmsMessage.getJMSMessageID();
            if (value != null)
            {
                messageProperties.put(JmsConstants.JMS_MESSAGE_ID, value);
                messageProperties.put(MuleProperties.MULE_MESSAGE_ID_PROPERTY, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addExpirationProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            long value = jmsMessage.getJMSExpiration();
            messageProperties.put(JmsConstants.JMS_EXPIRATION, Long.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addDestinationProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            Destination value = jmsMessage.getJMSDestination();
            if (value != null)
            {
                messageProperties.put(JmsConstants.JMS_DESTINATION, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addDeliveryModeProperty(Message jmsMessage, Map<String, Object> messageProperties)
    {
        try
        {
            int value = jmsMessage.getJMSDeliveryMode();
            messageProperties.put(JmsConstants.JMS_DELIVERY_MODE, Integer.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }
    }

    protected void addCorrelationProperties(Message jmsMessage, MuleMessage muleMessage, 
        Map<String, Object> messageProperties)
    {
        try
        {
            String value = jmsMessage.getJMSCorrelationID();
            if (value != null)
            {
                messageProperties.put(JmsConstants.JMS_CORRELATION_ID, value);
                // this property is used my getCorrelationId in MuleMessage, but we want
                // it on the INBOUND scoped properties so don't use setCorrelationId
                messageProperties.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }
    }
}
