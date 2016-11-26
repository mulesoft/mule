/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import javax.jms.Message;

import org.slf4j.Logger;


/**
 * Operation that allows the user to perform an ACK over a {@link Message} produced by the current {@link JmsSession}
 *
 * @since 4.0
 */
public final class JmsAck {

  private static final Logger LOGGER = getLogger(JmsAck.class);

  /**
   * Allows the user to perform an ACK when the {@link AckMode#MANUAL} mode is elected while consuming the {@link Message}.
   * As per JMS Spec, performing an ACK over a single {@link Message} automatically works as an ACK for all the {@link Message}s
   * produced in the same {@link JmsSession}.
   * <p>
   * The {@code ackId} must refer to a {@link JmsSession} created using the current {@link JmsConnection}.
   * If the {@link JmsSession} or {@link JmsConnection} were closed, the ACK will fail.
   * If the {@code ackId} does not belong to a {@link JmsSession} created using the current {@link JmsConnection}
   *
   * @param connection the {@link JmsConnection} that created the {@link JmsSession} over which the ACK will be performed
   * @param ackId the {@link JmsSession#getAckId}
   * @throws JmsExtensionException if the {@link JmsSession} or {@link JmsConnection} were closed, or if the ID doesn't belong
   * to a session of the current connection
   */
  public void ack(@Connection JmsConnection connection, @Summary("The AckId of the Message to ACK") String ackId)
      throws JmsExtensionException {

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Performing ACK on session: " + ackId);
      }

      connection.doAck(ackId);

    } catch (Exception e) {
      LOGGER.error(format("An error occurred while acking a message with ID [%s]: ", ackId), e);
      throw new JmsExtensionException(createStaticMessage("An error occurred while trying to perform an ACK on Session with ID [%s]: ",
                                                          ackId),
                                      e);
    }
  }

}
