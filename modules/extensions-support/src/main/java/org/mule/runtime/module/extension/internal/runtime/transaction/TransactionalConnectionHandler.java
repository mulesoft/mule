/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.internal.connection.ConnectionHandlerAdapter;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

/**
 * A {@link ConnectionHandlerAdapter} to be used when a {@link TransactionalConnection} is participating on a transaction.
 *
 * @param <T> The generic type of the {@link TransactionalConnection}
 * @since 4.0
 */
public final class TransactionalConnectionHandler<T extends TransactionalConnection> implements ConnectionHandlerAdapter<T> {

  private final ExtensionTransactionalResource<T> resource;

  /**
   * Creates a new instance
   *
   * @param resource a {@link ExtensionTransactionalResource} which wraps the connection
   */
  public TransactionalConnectionHandler(ExtensionTransactionalResource<T> resource) {
    checkArgument(resource != null, "resource cannot be null");
    this.resource = resource;
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
  public void release() {

  }

  /**
   * Closes the connection making sure that the owning transaction is resolved. If the transaction is still active, then it rolls
   * it back
   *
   * @throws MuleException if anything goes wrong
   */
  @Override
  public synchronized void close() throws MuleException {
    ConnectionHandler<T> connectionHandler = resource.getConnectionHandler();
    checkArgument(connectionHandler instanceof ConnectionHandlerAdapter, "connectionHandlerAdapter was expected");

    if (!resource.isTransactionResolved()) {
      try {
        // TODO: MULE-8946 this methods should throw TransactionException
        resource.rollback();
      } catch (Exception e) {
        throw new TransactionException(e);
      }
    }

    ((ConnectionHandlerAdapter) connectionHandler).close();
  }
}
