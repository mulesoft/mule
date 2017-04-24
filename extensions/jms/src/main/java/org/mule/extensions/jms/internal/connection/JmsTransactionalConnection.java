/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection;

import static org.mule.extensions.jms.internal.connection.session.TransactionStatus.NONE;
import static org.mule.extensions.jms.internal.connection.session.TransactionStatus.STARTED;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.internal.JmsExtension;
import org.mule.extensions.jms.internal.connection.session.JmsSessionManager;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

import org.slf4j.Logger;

import java.util.Optional;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Implementation of the {@link JmsConnection} which implements {@link TransactionalConnection} for Transaction Support
 * in the {@link JmsExtension}
 *
 * @since 4.0
 */
public final class JmsTransactionalConnection extends JmsConnection implements TransactionalConnection {

  private static final Logger LOGGER = getLogger(JmsTransactionalConnection.class);
  private static final String COMMIT = "Commit";
  private static final String ROLLBACK = "Rollback";

  public JmsTransactionalConnection(JmsSupport jmsSupport, Connection connection, JmsSessionManager jmsSessionManager) {
    super(jmsSupport, connection, jmsSessionManager);
  }

  /**
   * Begins a new Transaction for a JMS Session indicating in the {@link JmsSessionManager} that the current
   * {@link Thread} is being part of a transaction.
   */
  @Override
  public void begin() throws Exception {
    jmsSessionManager.changeTransactionStatus(STARTED);
  }

  /**
   * Executes a commit action over the bound {@link JmsSession} to the current {@link Thread}
   */
  @Override
  public void commit() throws Exception {
    executeTransactionAction(COMMIT, Session::commit);
  }

  /**
   * Executes a rollback action over the bound {@link JmsSession} to the current {@link Thread}
   */
  @Override
  public void rollback() throws Exception {
    executeTransactionAction(ROLLBACK, Session::rollback);
  }

  private void executeTransactionAction(String action, SessionAction transactionalAction) throws JMSException {
    Optional<JmsSession> transactedSession = jmsSessionManager.getTransactedSession();
    checkState(transactedSession.isPresent(), "Unable to " + action + " transaction, the TX Session doesn't exist.");

    Session jmsSession = transactedSession.get().get();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("JMS Transaction " + action + " over Session [" + jmsSession + "]");
    }

    try {
      transactionalAction.execute(jmsSession);
    } finally {
      jmsSessionManager.changeTransactionStatus(NONE);
      jmsSessionManager.unbindSession();
    }
  }

  @FunctionalInterface
  private interface SessionAction {

    void execute(Session session) throws JMSException;
  }
}
