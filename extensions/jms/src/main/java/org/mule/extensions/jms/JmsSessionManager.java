/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.exception.JmsAckException;
import org.mule.extensions.jms.api.source.JmsListener;
import org.mule.extensions.jms.api.source.JmsListenerLock;
import org.slf4j.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manager that takes the responsibility of register the session information to be able to execute a manual
 * acknowledgement or a recover over a {@link Session}.
 * This is used when the {@link AckMode} is configured in {@link AckMode#MANUAL}
 *
 * @since 4.0
 */
public class JmsSessionManager {

  private static final Logger LOGGER = getLogger(JmsSessionManager.class);
  private final Map<String, SessionInformation> pendingSessions = new HashMap<>();

  /**
   * Registers the {@link Message} to the {@link Session} using the {@code ackId} in order to being
   * able later to perform a {@link AckMode#MANUAL} ACK
   *
   * @param ackId   the id associated to the {@link Session} used to create the {@link Message}
   * @param message the {@link Message} to use for executing the {@link Message#acknowledge}
   * @param jmsLock the optional {@link JmsListenerLock} to be able to unlock the {@link JmsListener}
   * @throws IllegalArgumentException if no Session was registered with the given AckId
   */
  public void registerMessageForAck(String ackId, Message message, Session session, JmsListenerLock jmsLock) {
    if (!pendingSessions.containsKey(ackId)) {
      pendingSessions.put(ackId, new SessionInformation(message, session, jmsLock));
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Registered Message for Session AckId [%s]", ackId));
    }
  }

  /**
   * Executes the {@link Message#acknowledge} on the latest {@link Message} associated to the {@link Session}
   * identified by the {@code ackId}
   *
   * @param ackId the id associated to the {@link Session} that should be ACKed
   * @throws JMSException if an error occurs during the ack
   */
  public void doAck(String ackId) throws JMSException {
    SessionInformation sessionInformation = pendingSessions.get(ackId);

    if (sessionInformation == null) {
      throw new JmsAckException(format("No pending acknowledgement with ackId [%s] exists in this Connection", ackId));
    }

    sessionInformation.getMessage().acknowledge();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Acknowledged Message for Session with AckId [%s]", ackId));
    }
  }

  /**
   * Executes the {@link Session#recover()} over the {@link Session} identified by the {@code ackId}
   *
   * @param ackId the id associated to the {@link Session} used to create the {@link Message}
   * @throws JMSException if an error occurs during recovering the session
   */
  public void recoverSession(String ackId) throws JMSException {
    SessionInformation sessionInformation = pendingSessions.get(ackId);

    if (sessionInformation == null) {
      throw new JmsAckException(format("No pending session with ackId [%s] exists in this Connection", ackId));
    }

    Session session = sessionInformation.getSession();

    sessionInformation.getJmsListenerLock().ifPresent(lock -> {
      if (lock.isLocked()) {
        lock.unlock();
      }
    });

    session.recover();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Recovered session for AckId [%s]", ackId));
    }
  }

  private class SessionInformation {

    private Message message;
    private Session session;
    private JmsListenerLock jmsListenerLock;

    SessionInformation(Message message, Session session, JmsListenerLock jmsListenerLock) {
      this.message = message;
      this.session = session;
      this.jmsListenerLock = jmsListenerLock;
    }

    Message getMessage() {
      return message;
    }

    Session getSession() {
      return session;
    }

    Optional<JmsListenerLock> getJmsListenerLock() {
      return ofNullable(jmsListenerLock);
    }

  }
}
