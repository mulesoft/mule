/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.connection.ConnectionHandlerAdapter;
import org.mule.sdk.api.connectivity.TransactionalConnection;

import org.slf4j.Logger;

/**
 * A {@link ConnectionHandlerAdapter} to be used when a {@link TransactionalConnection} is participating on a transaction.
 *
 * @param <T> The generic type of the {@link TransactionalConnection}
 * @since 4.0
 */
public final class TransactionalConnectionHandler<T extends TransactionalConnection> implements ConnectionHandlerAdapter<T> {

  private static final Logger LOGGER = getLogger(TransactionalConnectionHandler.class);

  private final ExtensionTransactionalResource<T> resource;
  private final ConnectionHandlerAdapter<T> connectionHandler;

  /**
   * Creates a new instance
   *
   * @param resource a {@link ExtensionTransactionalResource} which wraps the connection
   */
  public TransactionalConnectionHandler(ExtensionTransactionalResource<T> resource) {
    this.resource = requireNonNull(resource, "resource cannot be null");
    this.connectionHandler = requireNonNull((ConnectionHandlerAdapter<T>) resource.getConnectionHandler());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getConnection() throws ConnectionException {
    return resource.getConnection();
  }

  /**
   * Does nothing since the connection shouldn't be released until the transaction is resolved
   */
  @Override
  public void release() {}

  @Override
  public void invalidate() {
    try {
      forceRollback();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Failed to rollback transaction while invalidating connection %s. %s", e, e.getMessage()), e);
      }
    } finally {
      connectionHandler.invalidate();
    }
  }

  /**
   * Closes the connection making sure that the owning transaction is resolved. If the transaction is still active, then it rolls
   * it back
   *
   * @throws MuleException if anything goes wrong
   */
  @Override
  public void close() throws MuleException {
    try {
      forceRollback();
    } finally {
      connectionHandler.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<T> getConnectionProvider() {
    return connectionHandler.getConnectionProvider();
  }

  private void forceRollback() throws TransactionException {
    try {
      TransactionCoordination.getInstance().rollbackCurrentTransaction();
    } catch (Exception e) {
      throw new TransactionException(e);
    }
  }
}
