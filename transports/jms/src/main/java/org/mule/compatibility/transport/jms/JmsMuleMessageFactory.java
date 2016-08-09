/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.mule.compatibility.transport.jms.JmsConstants.JMS_REPLY_TO;
import org.mule.compatibility.core.transport.AbstractMuleMessageFactory;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsMuleMessageFactory extends AbstractMuleMessageFactory {

  private static final Logger logger = LoggerFactory.getLogger(JmsMuleMessageFactory.class);

  @Override
  protected Class<?>[] getSupportedTransportMessageTypes() {
    return new Class[] {Message.class};
  }

  @Override
  protected Object extractPayload(Object transportMessage, Charset encoding) throws Exception {
    return transportMessage;
  }

  @Override
  protected void addProperties(MuleMessage.Builder messageBuilder, Object transportMessage) throws Exception {
    Message jmsMessage = (Message) transportMessage;

    Map<String, Serializable> messageProperties = new HashMap<>();
    addDeliveryModeProperty(jmsMessage, messageProperties);
    addExpirationProperty(jmsMessage, messageProperties);
    addMessageIdProperty(jmsMessage, messageProperties);
    addPriorityProperty(jmsMessage, messageProperties);
    addRedeliveredProperty(jmsMessage, messageProperties);
    addJMSReplyTo(messageBuilder, jmsMessage);
    addTimestampProperty(jmsMessage, messageProperties);
    addTypeProperty(jmsMessage, messageProperties);
    propagateJMSProperties(jmsMessage, messageProperties);

    addCorrelationProperties(jmsMessage, messageBuilder, messageProperties);

    messageProperties.forEach((k, v) -> messageBuilder.addInboundProperty(k, v));
  }

  protected void propagateJMSProperties(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      Enumeration<?> e = jmsMessage.getPropertyNames();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        try {
          Object value = jmsMessage.getObjectProperty(key);
          if (value != null) {
            if (value instanceof Serializable) {
              messageProperties.put(key, (Serializable) value);
            } else {
              logger.warn("The JMS property" + key + " is not serializable and will not be propagated by " + "Mule");
            }
          }
        } catch (JMSException e1) {
          // ignored
        }
      }
    } catch (JMSException e1) {
      // ignored
    }
  }

  protected void addTypeProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      String value = jmsMessage.getJMSType();
      if (value != null) {
        messageProperties.put(JmsConstants.JMS_TYPE, value);
      }
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addTimestampProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      long value = jmsMessage.getJMSTimestamp();
      messageProperties.put(JmsConstants.JMS_TIMESTAMP, Long.valueOf(value));
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addJMSReplyTo(MuleMessage.Builder messageBuilder, Message jmsMessage) {
    try {
      Destination replyTo = jmsMessage.getJMSReplyTo();
      if (replyTo != null) {
        if (!(replyTo instanceof Serializable)) {
          logger.warn("ReplyTo " + replyTo + " is not serializable and will not be propagated by Mule");
        }
        messageBuilder.addInboundProperty(JMS_REPLY_TO, (Serializable) replyTo);
      }
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addRedeliveredProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      boolean value = jmsMessage.getJMSRedelivered();
      messageProperties.put(JmsConstants.JMS_REDELIVERED, Boolean.valueOf(value));
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addPriorityProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      int value = jmsMessage.getJMSPriority();
      messageProperties.put(JmsConstants.JMS_PRIORITY, Integer.valueOf(value));
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addMessageIdProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      String value = jmsMessage.getJMSMessageID();
      if (value != null) {
        messageProperties.put(JmsConstants.JMS_MESSAGE_ID, value);
        messageProperties.put(MuleProperties.MULE_MESSAGE_ID_PROPERTY, value);
      }
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addExpirationProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      long value = jmsMessage.getJMSExpiration();
      messageProperties.put(JmsConstants.JMS_EXPIRATION, Long.valueOf(value));
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addDeliveryModeProperty(Message jmsMessage, Map<String, Serializable> messageProperties) {
    try {
      int value = jmsMessage.getJMSDeliveryMode();
      messageProperties.put(JmsConstants.JMS_DELIVERY_MODE, Integer.valueOf(value));
    } catch (JMSException e) {
      // ignored
    }
  }

  protected void addCorrelationProperties(Message jmsMessage, MuleMessage.Builder messageBuilder,
                                          Map<String, Serializable> messageProperties) {
    try {
      String value = jmsMessage.getJMSCorrelationID();
      if (value != null) {
        messageBuilder.addInboundProperty(JmsConstants.JMS_CORRELATION_ID, value);
        messageBuilder.correlationId(value);
      }

      final Serializable mcid = messageProperties.remove(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
      if (mcid != null) {
        messageBuilder.correlationId(mcid.toString());
      }
    } catch (JMSException e) {
      // ignored
    }
  }
}
