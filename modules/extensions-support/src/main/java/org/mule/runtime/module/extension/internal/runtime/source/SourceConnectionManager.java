/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.Source;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and manages the connections that are started on a {@link Source}
 *
 * @since 4.0
 */
public class SourceConnectionManager {

  private final ConnectionManager connectionManager;
  private final Map<Reference<Object>, ConnectionHandler<Object>> connections = new ConcurrentHashMap<>();

  /**
   * Creates a new instance
   *
   * @param connectionManager the {@link ConnectionManager} which actually creates the connections
   */
  SourceConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Obtains a new connection for the given {@code config}
   *
   * @param config the config that will own the connection
   * @param <T>    the connection's generic type
   * @return the obtained connection
   * @throws ConnectionException if a connection could not be obtained
   */
  <T> T getConnection(ConfigurationInstance config) throws ConnectionException {
    ConnectionHandler<Object> connectionHandler = connectionManager.getConnection(config.getValue());
    Object connection = connectionHandler.getConnection();
    connections.put(new Reference<>(connection), connectionHandler);

    return (T) connection;
  }

  /**
   * Releases the given connection
   *
   * @param connection the connection to be released
   */
  void release(Object connection) {
    ConnectionHandler<Object> connectionHandler = connections.remove(new Reference<>(connection));
    if (connectionHandler != null) {
      connectionHandler.release();
    }
  }

  /**
   * Invalidates the given connection
   *
   * @param connection the connection to be invalidated
   */
  void invalidate(Object connection) {
    ConnectionHandler<Object> connectionHandler = connections.remove(new Reference<>(connection));
    if (connectionHandler != null) {
      connectionHandler.invalidate();
    }
  }

  /**
   * Tests that the given {@code connectionProvider} is capable of producing a valid connection
   *
   * @param connection the connection to validate
   * @return a {@link ConnectionValidationResult}
   */
  ConnectionValidationResult testConnectivity(Object connection) {
    ConnectionHandler<Object> connectionHandler = connections.get(new Reference<>(connection));
    if (connectionHandler == null) {
      throw new IllegalArgumentException("Cannot validate a connection which was not generated through this "
          + ConnectionProvider.class.getSimpleName());
    }

    return connectionManager.testConnectivity(connection, connectionHandler);
  }

  /**
   * Returns the {@link ConnectionHandler} from which the given {@code connection} was obtained
   *
   * @param connection a connection
   * @param <T>        the connection's generic type
   * @return an {@link Optional} {@link ConnectionHandler}, {@link Optional#empty()} if the connection was not obtained
   * through this component
   */
  <T> Optional<ConnectionHandler<T>> getConnectionHandler(T connection) {
    return ofNullable((ConnectionHandler<T>) connections.get(new Reference<>(connection)));
  }
}
