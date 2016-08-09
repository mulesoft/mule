/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionHandlerAdapter} which always returns the same connection (therefore cached), which is not established until
 * {@link #getConnection()} is first invoked.
 * <p/>
 * This implementation is thread-safe.
 *
 * @param <Connection> the generic type of the connection being wrapped
 * @since 4.0
 */
final class CachedConnectionHandler<Connection> implements ConnectionHandlerAdapter<Connection> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedConnectionHandler.class);

  private final ConnectionProvider<Connection> connectionProvider;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final MuleContext muleContext;

  private Connection connection;

  /**
   * Creates a new instance
   *
   * @param connectionProvider the {@link ConnectionProvider} to be used to managed the connection
   * @param muleContext the owning {@link MuleContext}
   */
  public CachedConnectionHandler(ConnectionProvider<Connection> connectionProvider, MuleContext muleContext) {
    this.connectionProvider = connectionProvider;
    this.muleContext = muleContext;
  }

  /**
   * On the first invocation to this method, a connection is established using the provided {@link #connectionProvider}. That
   * connection is cached and returned.
   * <p/>
   * Following invocations simply return the same connection.
   *
   * @return a {@code Connection}
   * @throws ConnectionException if a {@code Connection} could not be obtained
   * @throws IllegalStateException if the first invocation is executed while the {@link #muleContext} is stopping or stopped
   */
  @Override
  public Connection getConnection() throws ConnectionException {
    Connection oldConnection;
    readLock.lock();
    try {
      if (connection != null && validateConnection(connection).isValid()) {
        return connection;
      }
      oldConnection = connection;
    } finally {
      readLock.unlock();
    }
    writeLock.lock();
    try {
      // check another thread didn't beat us to it
      if (connection != null) {
        if (connection != oldConnection) {
          return connection;
        }
        disconnectAndCleanConnection(connection);
      }
      connection = createConnection();
      return connection;
    } finally {
      writeLock.unlock();
    }
  }

  private Connection createConnection() throws ConnectionException {
    assertNotStopping(muleContext, "Mule is shutting down... Cannot establish new connections");
    connection = connectionProvider.connect();
    return connection;
  }

  /**
   * This implementation doesn't require the concept of release. This method does nothing
   */
  @Override
  public void release() {
    // no-op
  }

  /**
   * Disconnects the wrapped connection and clears the cache
   *
   * @throws MuleException in case of error
   */
  @Override
  public void close() throws MuleException {
    writeLock.lock();
    try {
      if (connectionProvider != null && connection != null) {
        connectionProvider.disconnect(connection);
      }
    } finally {
      connection = null;
      writeLock.unlock();
    }
  }

  protected ConnectionValidationResult validateConnection(Connection connection) {
    ConnectionValidationResult validationResult = null;
    try {
      validationResult = connectionProvider.validate(connection);
    } catch (Exception e) {
      validationResult = ConnectionValidationResult.failure(
                                                            "Error validating connection. Unexpected exception was thrown by the extension when validating the connection",
                                                            ConnectionExceptionCode.UNKNOWN, e);
    }

    if (validationResult == null) {
      String errorMessage =
          "Error validating connection. validate() method from the connection provider can not return a null ConnectionValidationResult";
      validationResult = ConnectionValidationResult.failure(errorMessage, ConnectionExceptionCode.UNKNOWN,
                                                            new ConnectionException(errorMessage));
    }

    return validationResult;
  }

  private void disconnectAndCleanConnection(Connection connection) {
    try {
      connectionProvider.disconnect(connection);
    } catch (Exception e) {
      LOGGER.debug("Error disconnecting the extension's connection", e);
    } finally {
      this.connection = null;
    }
  }
}
