/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;

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

  public static final String COULD_NOT_OBTAIN_A_CONNECTION = "Could not obtain a connection for the configuration";
  private PagingProvider<Object, T> delegate;
  private final ConfigurationInstance config;
  private final ExtensionConnectionSupplier connectionSupplier;
  private final ExecutionContextAdapter executionContext;
  private final ConnectionSupplierFactory connectionSupplierFactory;

  public PagingProviderProducer(PagingProvider<Object, T> delegate,
                                ConfigurationInstance config,
                                ExecutionContextAdapter executionContext,
                                ExtensionConnectionSupplier connectionSupplier) {
    this.delegate = new PagingProviderWrapper(delegate);
    this.config = config;
    this.executionContext = executionContext;
    this.connectionSupplier = connectionSupplier;

    this.connectionSupplierFactory = createConnectionSupplierFactory();
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
  public int getSize() {
    return performWithConnection(connection -> delegate.getTotalResults(connection)).orElse(-1);
  }

  /**
   * Finds a connection and applies the {@link Function} passed as parameter.
   *
   * @param function a function that receives a connection as input and returns a value.
   * @param <R>      the return type of the function
   * @return
   */
  private <R> R performWithConnection(Function<Object, R> function) {
    ConnectionSupplier connectionSupplier = null;
    try {
      connectionSupplier = connectionSupplierFactory.getConnectionSupplier();
      return function.apply(connectionSupplier.getConnection());
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_OBTAIN_A_CONNECTION), e);
    } finally {
      if (connectionSupplier != null) {
        connectionSupplier.close();
      }
    }
  }

  /**
   * Closes the delegate
   */
  @Override
  public void close() throws IOException {
    try {
      ConnectionSupplier connectionSupplier = connectionSupplierFactory.getConnectionSupplier();
      delegate.close(connectionSupplier.getConnection());
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_OBTAIN_A_CONNECTION), e);
    } finally {
      connectionSupplierFactory.dispose();
    }
  }

  private ConnectionSupplierFactory createConnectionSupplierFactory() {
    if (delegate.useStickyConnections() || isTransactional()) {
      return new StickyConnectionSupplierFactory();
    }

    return new DefaultConnectionSupplierFactory();
  }

  private boolean isTransactional() {
    Transaction tx = TransactionCoordination.getInstance().getTransaction();
    return tx != null && tx.hasResource(new ExtensionTransactionKey(config));
  }

  private interface ConnectionSupplierFactory {

    ConnectionSupplier getConnectionSupplier() throws MuleException;

    void dispose();
  }


  private class DefaultConnectionSupplierFactory implements ConnectionSupplierFactory {

    @Override
    public ConnectionSupplier getConnectionSupplier() throws MuleException {
      return new DefaultConnectionSupplier(connectionSupplier.getConnection(executionContext));
    }

    @Override
    public void dispose() {

    }
  }


  private class StickyConnectionSupplierFactory implements ConnectionSupplierFactory {

    private final LazyValue<ConnectionSupplier> stickyConnection = new LazyValue<>(new CheckedSupplier<ConnectionSupplier>() {

      @Override
      public ConnectionSupplier getChecked() throws Throwable {
        StickyConnectionSupplierFactory.this.connectionHandler = connectionSupplier.getConnection(executionContext);
        return new StickyConnectionSupplier(StickyConnectionSupplierFactory.this.connectionHandler.getConnection());
      }
    });

    private ConnectionHandler connectionHandler;

    @Override
    public ConnectionSupplier getConnectionSupplier() throws MuleException {
      return stickyConnection.get();
    }

    @Override
    public void dispose() {
      if (connectionHandler != null) {
        connectionHandler.release();
      }
    }
  }


  private interface ConnectionSupplier {

    Object getConnection() throws MuleException;

    void close();
  }


  private class DefaultConnectionSupplier implements ConnectionSupplier {

    private final ConnectionHandler connectionHandler;

    public DefaultConnectionSupplier(ConnectionHandler connectionHandler) {
      this.connectionHandler = connectionHandler;
    }

    public Object getConnection() throws MuleException {
      return connectionHandler.getConnection();
    }

    public void close() {
      connectionHandler.release();
    }
  }


  private class StickyConnectionSupplier implements ConnectionSupplier {

    private final Object connection;

    public StickyConnectionSupplier(Object connection) {
      this.connection = connection;
    }

    @Override
    public Object getConnection() throws MuleException {
      return connection;
    }

    @Override
    public void close() {

    }
  }
}
