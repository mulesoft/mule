/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.streaming.Producer;
import org.mule.runtime.extension.api.introspection.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * Implementation of {@link Producer} that uses an instance of {@link PagingProvider} to get its results.
 *
 * @param <T> the type of the elements returned by the {@link PagingProvider}.
 * @since 3.5.0
 */
public final class PagingProviderProducer<T> implements Producer<List<T>> {

  private PagingProvider<Object, T> delegate;
  private final ConfigurationInstance config;
  private final ConnectionManager connectionManager;

  public PagingProviderProducer(PagingProvider<Object, T> delegate, ConfigurationInstance config, ConnectionManager manager) {
    this.delegate = new PagingProviderWrapper(delegate);
    this.config = config;
    this.connectionManager = manager;
  }

  /**
   * Asks the delegate for the next page
   */
  @Override
  public List<T> produce() {
    return performWithConnection(connection -> delegate.getPage(connection));
  }

  /**
   * Returns the total amount of available results informed by delegate
   */
  @Override
  public int size() {
    return performWithConnection(connection -> delegate.getTotalResults(connection)).orElse(-1);
  }

  /**
   * Finds a connection and applies the {@link Function} passed as parameter.
   *
   * @param function a function that receives a connection as input and returns a value.
   * @param <R> the return type of the function
   * @return
   */
  private <R> R performWithConnection(Function<Object, R> function) {
    ConnectionHandler connectionHandler = null;
    try {
      connectionHandler = connectionManager.getConnection(config.getValue());
      return function.apply(connectionHandler.getConnection());
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not obtain a connection for the configuration"), e);
    } finally {
      if (connectionHandler != null) {
        connectionHandler.release();
      }
    }
  }

  /**
   * Closes the delegate
   */
  @Override
  public void close() throws IOException {
    this.delegate.close();
  }
}
