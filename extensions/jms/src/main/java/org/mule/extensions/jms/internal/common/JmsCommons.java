/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.common;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.api.config.AckMode.NONE;
import static org.mule.extensions.jms.api.message.MessageBuilder.BODY_CONTENT_TYPE_JMS_PROPERTY;
import static org.mule.extensions.jms.api.message.MessageBuilder.BODY_ENCODING_JMS_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.JmsSessionManager;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.exception.JmsAckException;
import org.mule.extensions.jms.api.source.JmsListenerLock;
import org.slf4j.Logger;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Utility class for Jms Operations
 *
 * @since 4.0
 */
public final class JmsCommons {

  private static final Logger LOGGER = getLogger(JmsCommons.class);

  public static final String EXAMPLE_ENCODING = "UTF-8";
  public static final String EXAMPLE_CONTENT_TYPE = "application/json";

  public static String resolveMessageContentType(Message message, String defaultType) {
    try {
      String contentType = message.getStringProperty(BODY_CONTENT_TYPE_JMS_PROPERTY);
      return isBlank(contentType) ? defaultType : contentType;
    } catch (JMSException e) {
      LOGGER.warn(format("Failed to read the Message ContentType from its properties. A default value of [%s] will be used.",
                         defaultType));
      return defaultType;
    }
  }

  public static String resolveMessageEncoding(Message message, String defaultType) {
    try {
      String contentType = message.getStringProperty(BODY_ENCODING_JMS_PROPERTY);
      return isBlank(contentType) ? defaultType : contentType;
    } catch (JMSException e) {
      LOGGER.warn(format("Failed to read the Message ContentType from its properties. A default value of [%s] will be used.",
                         defaultType));
      return defaultType;
    }
  }

  public static <T> T resolveOverride(T configValue, T operationValue) {
    return operationValue == null ? configValue : operationValue;
  }

  public static void evaluateMessageAck(AckMode ackMode, JmsSession session, Message receivedMessage,
                                        JmsSessionManager messageSessionManager, JmsListenerLock jmsLock)
      throws JMSException {
    try {
      if (ackMode.equals(NONE)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Automatically performing an ACK over the message, since AckMode was NONE");
        }
        receivedMessage.acknowledge();

      } else if (ackMode.equals(MANUAL)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Registering pending ACK on session: " + session.getAckId());
        }
        String id = session.getAckId()
            .orElseThrow(() -> new IllegalArgumentException("An AckId is required when MANUAL AckMode is set"));

        messageSessionManager.registerMessageForAck(id, receivedMessage, session.get(), jmsLock);
      }
    } catch (JMSException e) {
      throw new JmsAckException("An error occurred while acknowledging the message", e);
    }
  }

}
