/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.lang.String.format;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionHandlerAdapter} which always returns the same connection (therefore cached), which is not established until
 * {@link #getConnection()} is first invoked.
 * <p/>
 * This implementation is thread-safe.
 *
 * @param <C> the generic type of the connection being wrapped
 * @since 4.0
 */
public final class CachedConnectionHandler<C> implements ConnectionHandlerAdapter<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedConnectionHandler.class);

  private final Consumer<ConnectionHandlerAdapter<C>> releaser;
  private final ConnectionProvider<C> connectionProvider;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean invalidated = new AtomicBoolean(false);

  private C connection;

  public CachedConnectionHandler(C connection, Consumer<ConnectionHandlerAdapter<C>> releaser,
                                 ConnectionProvider<C> connectionProvider) {
    this.connection = connection;
    this.releaser = releaser;
    this.connectionProvider = connectionProvider;
  }

  @Override
  public C getConnection() throws ConnectionException {
    return connection;
  }

  /**
   * This implementation doesn't require the concept of release. This method does nothing
   */
  @Override
  public void release() {
    if (connection instanceof XATransactionalConnection xaTxConnection) {
      // Legacy XA support: maintain compatibility with current (1.x) versions of JMS module
      // (this is the only occurrence of XA using cached connections).
      // This will not close the connection: it will close the BTM wrapped connection that will return it to the pool.
      xaTxConnection.close();
    }
    // else the cached connection is kept as is so it can continue to be used
  }

  /**
   * Disconnects the wrapped connection and clears the cache
   *
   * @throws MuleException in case of error
   */
  @Override
  public void close() throws MuleException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    try {
      connectionProvider.disconnect(connection);
    } catch (Exception e) {
      LOGGER.warn("Error disconnecting cached connection {}. {}", connection, e.getMessage(), e);
    } finally {
      connection = null;
    }
  }

  @Override
  public void invalidate() {
    if (invalidated.compareAndSet(false, true)) {
      try {
        close();
      } catch (Exception e) {
        LOGGER.warn("Error invalidating cached connection {}. {}", connection, e.getMessage(), e);
      } finally {
        releaser.accept(this);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<C> getConnectionProvider() {
    return connectionProvider;
  }
}
