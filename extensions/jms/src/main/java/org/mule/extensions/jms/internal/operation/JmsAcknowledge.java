/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.operation;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.internal.connection.session.JmsSessionManager;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.internal.connection.JmsConnection;
import org.mule.extensions.jms.internal.connection.JmsSession;
import org.mule.extensions.jms.api.exception.JmsAckErrorTypeProvider;
import org.mule.extensions.jms.api.exception.JmsAckException;
import org.mule.extensions.jms.api.exception.JmsSessionRecoverErrorTypeProvider;
import org.mule.extensions.jms.api.exception.JmsSessionRecoverException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jms.Message;


/**
 * Operation that allows the user to perform an ACK over a {@link Message} produced by the current {@link JmsSession}
 *
 * @since 4.0
 */
public final class JmsAcknowledge {

  private static final Logger LOGGER = getLogger(JmsAcknowledge.class);

  @Inject
  private JmsSessionManager sessionManager;

  /**
   * Allows the user to perform an ACK when the {@link AckMode#MANUAL} mode is elected while consuming the {@link Message}.
   * As per JMS Spec, performing an ACK over a single {@link Message} automatically works as an ACK for all the {@link Message}s
   * produced in the same {@link JmsSession}.
   *
   * @param ackId The AckId of the Message to ACK
   * @throws JmsAckException if the {@link JmsSession} or {@link JmsConnection} were closed, or if the ID doesn't belong
   * to a session of the current connection
   */
  @Throws(JmsAckErrorTypeProvider.class)
  public void ack(@Summary("The AckId of the Message to ACK") String ackId) {
    checkArgument(!isBlank(ackId), "The AckId can not be null or empty");
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Performing ACK on session: " + ackId);
      }

      sessionManager.ack(ackId);

    } catch (Exception e) {
      LOGGER.error(format("An error occurred while acking a message with ID [%s]: ", ackId), e);
      throw new JmsAckException(format("An error occurred while trying to perform an ACK on Session with ID [%s]: ", ackId), e);
    }
  }

  /**
   * Allows the user to perform a session recover when the {@link AckMode#MANUAL} mode is elected while consuming the
   * {@link Message}.
   * As per JMS Spec, performing a session recover automatically will redeliver all the consumed messages that had not being
   * acknowledged before this recover.
   *
   * @param ackId The AckId of the Message Session to recover
   */
  @Throws(JmsSessionRecoverErrorTypeProvider.class)
  public void recoverSession(String ackId) {
    checkArgument(!isBlank(ackId), "The AckId can not be null or empty");
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Recovering session: " + ackId);
      }

      sessionManager.recoverSession(ackId);

    } catch (Exception e) {
      LOGGER.error(format("An error occurred while recovering the session with ID [%s]: ", ackId), e);
      throw new JmsSessionRecoverException(format("An error occurred while trying to perform an recover on Session with ID [%s]: ",
                                                  ackId),
                                           e);
    }
  }
}
