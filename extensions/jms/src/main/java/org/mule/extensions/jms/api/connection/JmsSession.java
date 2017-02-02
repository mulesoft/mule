/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Session;

import org.slf4j.Logger;

/**
 * Wrapper element for a JMS {@link Session} that relates the
 * session with its AckID
 *
 * @since 4.0
 */
public final class JmsSession implements AutoCloseable {

  private static final Logger LOGGER = getLogger(JmsSession.class);

  private final Session session;
  private String ackId;

  public JmsSession(Session session) {
    checkArgument(session != null, "A non null Session is required to use as delegate");
    this.session = session;
  }

  public JmsSession(Session session, String ackId) {
    this.session = session;
    this.ackId = ackId;
  }

  /**
   * @return the JMS {@link Session}
   */
  public Session get() {
    return session;
  }

  /**
   * @return the AckId of this {@link Session} or {@link Optional#empty} if no AckId is required
   */
  public Optional<String> getAckId() {
    return ofNullable(ackId);
  }

  @Override
  public void close() throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing session " + session);
    }
    session.close();
  }
}
