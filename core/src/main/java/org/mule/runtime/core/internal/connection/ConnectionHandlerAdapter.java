/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.Closeable;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.connection.ConnectionHandler;

/**
 * Adapter interface which extends the {@link ConnectionHandler} contract with non-API functionality
 *
 * @param <T> the generic type of the wrapped connection
 * @since 4.0
 */
public interface ConnectionHandlerAdapter<T> extends ConnectionHandler<T>, Closeable {

  /**
   * @return The {@link ConnectionProvider} which produced the connection
   */
  ConnectionProvider<T> getConnectionProvider();

  /**
   * Indicates that all resources allocated by the wrapped connection must be closed and released. This is different from
   * {@link #release()} in the sense that the latter doesn't specify if the connection's resources are actually freed or not. This
   * on the other hand is pretty explicit about that and implies that {@code this} instance is no longer usable after invoking
   * this method
   *
   * @throws MuleException if an exception occurs closing the resource
   */
  @Override
  void close() throws MuleException;
}
