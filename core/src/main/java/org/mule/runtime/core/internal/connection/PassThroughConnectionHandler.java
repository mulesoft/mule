/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link ConnectionHandlerAdapter} which doesn't perform any management.
 * <p>
 * {@code this} instance is created associated to a specific instance which is returned on every invokation to
 * {@link #getConnection()}. Calling {@link #release()} or {@link #close()} means that
 * {@link ConnectionProvider#disconnect(Object)} will be invoked on {@link #connectionProvider} with {@link #connection} as
 * argument.
 *
 * @param <Connection> the generic type of the connections to be handled
 * @since 4.0
 */
final class PassThroughConnectionHandler<Connection> implements ConnectionHandlerAdapter<Connection> {

  private final Connection connection;
  private final ConnectionProvider<Connection> connectionProvider;
  private final AtomicBoolean released = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param connection the connection to be returned by {@link #getConnection()}
   * @param connectionProvider the {@link ConnectionProvider} used to manage the connection
   */
  PassThroughConnectionHandler(Connection connection, ConnectionProvider<Connection> connectionProvider) {
    this.connection = connection;
    this.connectionProvider = connectionProvider;
  }

  /**
   * @return {@link #connection}
   */
  @Override
  public Connection getConnection() throws ConnectionException {
    return connection;
  }

  /**
   * Delegates into {@link #release()}
   */
  @Override
  public void close() throws MuleException {
    release();
  }

  /**
   * Invokes {@link ConnectionProvider#disconnect(Object)} on {@link #connectionProvider} using {@link #connection} as an
   * argument. This method is thread safe, {@link #connection} will not be disconnected twice
   */
  @Override
  public void release() {
    if (released.compareAndSet(false, true)) {
      connectionProvider.disconnect(connection);
    }
  }
}
