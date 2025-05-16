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

final class XaManagementWrappedConnectionProvider<C> implements ConnectionProvider<C> {

  private final ConnectionProvider<C> baseConnectionProvider;
  private final ConnectionManagementStrategy<C> xaMgmtStrategy;
  private final Map<C, ConnectionHandler<C>> handlers = new ConcurrentHashMap<>();

  XaManagementWrappedConnectionProvider(ConnectionProvider<C> baseConnectionProvider,
                                        ConnectionManagementStrategy<C> xaMgmtStrategy) {
    this.baseConnectionProvider = requireNonNull(baseConnectionProvider);
    this.xaMgmtStrategy = requireNonNull(xaMgmtStrategy);
  }

  @Override
  public C connect() throws ConnectionException {
    final var connectionHandler = xaMgmtStrategy.getConnectionHandler();
    final var connection = connectionHandler.getConnection();
    handlers.put(connection, connectionHandler);
    return connection;
  }

  @Override
  public void disconnect(C connection) {
    handlers.remove(connection).release();
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    return baseConnectionProvider.validate(connection);
  }
}
