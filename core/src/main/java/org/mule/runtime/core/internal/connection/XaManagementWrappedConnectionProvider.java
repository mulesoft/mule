/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class XaManagementWrappedConnectionProvider<TxC> implements ConnectionProvider<TxC> {

  private final ConnectionProvider<TxC> baseConnectionProvider;
  private final ConnectionManagementStrategy<TxC> xaMgmtStrategy;
  private final Map<TxC, ConnectionHandler<TxC>> handlers = new ConcurrentHashMap<>();

  XaManagementWrappedConnectionProvider(ConnectionProvider<TxC> baseConnectionProvider,
                                        ConnectionManagementStrategy<TxC> xaMgmtStrategy) {
    this.baseConnectionProvider = requireNonNull(baseConnectionProvider);
    this.xaMgmtStrategy = requireNonNull(xaMgmtStrategy);
  }

  @Override
  public TxC connect() throws ConnectionException {
    final var connectionHandler = xaMgmtStrategy.getConnectionHandler();
    final var connection = connectionHandler.getConnection();
    handlers.put(connection, connectionHandler);
    return connection;
  }

  @Override
  public void disconnect(TxC connection) {
    handlers.remove(connection).release();
  }

  @Override
  public ConnectionValidationResult validate(TxC connection) {
    return baseConnectionProvider.validate(connection);
  }
}
