/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;

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
final class CachedConnectionHandler<C> implements ConnectionHandlerAdapter<C> {

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
    // no-op
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
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(String.format("Error disconnecting cached connection %s. %s", connection, e.getMessage()), e);
      }
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
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(String.format("Error invalidating cached connection %s. %s", connection, e.getMessage()), e);
        }
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
