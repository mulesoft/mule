/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.source.JmsListener;
import org.mule.extensions.jms.api.source.JmsListenerLock;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

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
      LOGGER.debug("Registered Message for Session AckId [" + ackId + "]");
    }
  }

  /**
   * Executes the {@link Message#acknowledge} on the latest {@link Message} associated to the {@link Session}
   * identified by the {@code ackId}
   *
   * @param ackId the id associated to the {@link Session} that should be ACKed
   * @throws JMSException if an error occurs during the ack
   */
  public void ack(String ackId) throws JMSException {
    Optional<SessionInformation> optionalSession = getSessionInformation(ackId);

    if (optionalSession.isPresent()) {
      optionalSession.get().getMessage().acknowledge();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Acknowledged Message for Session with AckId [" + ackId + "]");
      }
    } else {
      //TODO - MULE-11963 : Improve error message for JmsAcknowledgement operations when the SessionInformation doesn't exist anymore
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("The session could not be acknowledged. This may be due to: \n " +
            "- The session has been already acknowledged\n" +
            "- The session has been recovered\n " +
            "- The given 'ackId' :  [" + ackId + "] is invalid.");
      }
    }
  }

  /**
   * Executes the {@link Session#recover()} over the {@link Session} identified by the {@code ackId}
   *
   * @param ackId the id associated to the {@link Session} used to create the {@link Message}
   * @throws JMSException if an error occurs during recovering the session
   */
  public void recoverSession(String ackId) throws JMSException {
    Optional<SessionInformation> optionalSession = getSessionInformation(ackId);
    if (optionalSession.isPresent()) {
      SessionInformation sessionInformation = optionalSession.get();

      sessionInformation.getJmsListenerLock().ifPresent(lock -> {
        if (lock.isLocked()) {
          lock.unlock();
        }
      });

      sessionInformation.getSession().recover();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Recovered session for AckId [ " + ackId + "]");
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        //TODO - MULE-11963 : Improve error message for JmsAcknowledgement operations when the SessionInformation doesn't exist anymore
        LOGGER.debug("The session could not be recovered, this could be due to: \n" +
            "- The session has been already recovered\n" +
            "- The all session messages has been already acknowledged\n" +
            "- The given 'ackId' : [" + ackId + "] is invalid");
      }
    }
  }

  private Optional<SessionInformation> getSessionInformation(String ackId) {
    return ofNullable(pendingSessions.remove(ackId));
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
