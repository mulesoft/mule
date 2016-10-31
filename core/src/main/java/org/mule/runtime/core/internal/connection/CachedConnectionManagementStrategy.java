/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;

/**
 * A {@link ConnectionManagementStrategy} which lazily creates and caches connections, so that the same instance is returned each
 * time that one is required.
 * <p/>
 * When {@link ConnectionHandler#release()} is invoked on the instances returned by {@link #getConnectionHandler()}, the
 * connection is not actually closed. It will only be disconnected when {@link #close()} is called.
 *
 * @param <C> the generic type of the connections being managed
 * @since 4.0
 */
final class CachedConnectionManagementStrategy<C> extends ConnectionManagementStrategy<C> {

  private final ConnectionHandlerAdapter<C> connection;

  /**
   * Creates a new instance
   *
   * @param connectionProvider the {@link ConnectionProvider} used to manage the connections
   * @param muleContext the owning {@link MuleContext}
   */
  CachedConnectionManagementStrategy(ConnectionProvider<C> connectionProvider, MuleContext muleContext) {
    super(connectionProvider, muleContext);
    connection = new CachedConnectionHandler<>(connectionProvider, muleContext);
  }

  /**
   * Returns the cached connection
   *
   * @return a {@link ConnectionHandler}
   * @throws ConnectionException if the connection could not be established
   */
  @Override
  public ConnectionHandler<C> getConnectionHandler() throws ConnectionException {
    return connection;
  }

  /**
   * Invokes {@link ConnectionHandlerAdapter#close()} on the cached connection
   *
   * @throws MuleException if an exception is found trying to close the connection
   */
  @Override
  public void close() throws MuleException {
    connection.close();
  }
}
