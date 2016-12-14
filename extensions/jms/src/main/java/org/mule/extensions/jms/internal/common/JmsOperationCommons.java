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
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;

/**
 * Utility class for Jms Operations
 *
 * @since 4.0
 */
public final class JmsOperationCommons {

  private static final Logger LOGGER = getLogger(JmsOperationCommons.class);

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

  public static <T> T resolveOverride(T configValue, T operationValue) {
    return operationValue == null ? configValue : operationValue;
  }

  public static void evaluateMessageAck(JmsConnection connection, AckMode ackMode, JmsSession session,
                                        Message received)
      throws JMSException {
    if (ackMode.equals(NONE)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Automatically performing an ACK over the message, since AckMode was NONE");
      }
      received.acknowledge();

    } else if (ackMode.equals(MANUAL)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Registering pending ACK on session: " + session.getAckId());
      }
      String id = session.getAckId()
          .orElseThrow(() -> new IllegalArgumentException("An AckId is required when MANUAL AckMode is set"));

      connection.registerMessageForAck(id, received);
    }
  }

}
