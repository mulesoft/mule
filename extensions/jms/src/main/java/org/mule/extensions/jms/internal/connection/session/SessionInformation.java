/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.session;

import static java.util.Optional.ofNullable;
import org.mule.extensions.jms.internal.source.JmsListenerLock;

import java.util.Optional;

import javax.jms.Message;
import javax.jms.Session;

/**
 * Object to save information about the relationship between a {@link Message}, their {@link Session} and
 * the {@link JmsListenerLock} waiting for the message to be processed.
 *
 * @since 4.0
 */
final class SessionInformation {

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
