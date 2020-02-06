/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;

import org.mule.runtime.api.connection.ConnectionException;
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

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Producer} that uses an instance of {@link PagingProvider} to get its results.
 *
 * @param <T> the type of the elements returned by the {@link PagingProvider}.
 * @since 3.5.0
 */
public final class PagingProviderProducer<T> implements Producer<List<T>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PagingProviderProducer.class);

  public static final String COULD_NOT_OBTAIN_A_CONNECTION = "Could not obtain a connection for the configuration";
  public static final String COULD_NOT_CREATE_A_CONNECTION_SUPPLIER =
      "Could not obtain a connection supplier for the configuration";
  public static final String COULD_NOT_CLOSE_PAGING_PROVIDER = "Could not close the Paging Provider";
  public static final String COULD_NOT_EXECUTE = "Could not execute operation with connection";
  private PagingProvider<Object, T> delegate;
  private final ConfigurationInstance config;
  private final ExtensionConnectionSupplier extensionConnectionSupplier;
  private final ExecutionContextAdapter executionContext;
  private final ConnectionSupplierFactory connectionSupplierFactory;
  private ConnectionSupplier connectionSupplier;
  private Boolean isFirstPage = true;

  public PagingProviderProducer(PagingProvider<Object, T> delegate,
                                ConfigurationInstance config,
                                ExecutionContextAdapter executionContext,
                                ExtensionConnectionSupplier extensionConnectionSupplier) {
    this.delegate = new PagingProviderWrapper(delegate, executionContext.getExtensionModel());
    this.config = config;
    this.executionContext = executionContext;
    this.extensionConnectionSupplier = extensionConnectionSupplier;

    this.connectionSupplierFactory = createConnectionSupplierFactory();
  }

  /**
   * Asks the delegate for the next page
   */
  @Override
  public List<T> produce() {
    List<T> page = performWithConnection(connection -> delegate.getPage(connection));
    isFirstPage = false;
    return page;
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
    connectionSupplier = getConnectionSupplier();
    Object connection = getConnection(connectionSupplier);
    try {
      R result = function.apply(connection);
      connectionSupplier.close();
      return result;
    } catch (Exception e) {
      if (isFirstPage) {
        closeDelegate(connection);
      }
      extractConnectionException(e).ifPresent(ex -> connectionSupplier.invalidateConnection());
      throw e;
    }
  }

  /**
   * Closes the delegate
   */
  @Override
  public void close() {
    try {
      closeDelegate(connectionSupplier.getConnection());
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_OBTAIN_A_CONNECTION), e);
    } finally {
      connectionSupplier.close();
      connectionSupplierFactory.dispose();
    }
  }

  private ConnectionSupplierFactory createConnectionSupplierFactory() {
    if (delegate.useStickyConnections() || isTransactional()) {
      return new StickyConnectionSupplierFactory();
    }

    return new DefaultConnectionSupplierFactory();
  }

  private ConnectionSupplier getConnectionSupplier() {
    try {
      return connectionSupplierFactory.getConnectionSupplier();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_CREATE_A_CONNECTION_SUPPLIER), e);
    }
  }

  private Object getConnection(ConnectionSupplier connectionSupplier) {
    try {
      return connectionSupplier.getConnection();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_OBTAIN_A_CONNECTION), e);
    }
  }

  private void closeDelegate(Object connection) {
    try {
      delegate.close(connection);
    } catch (Exception e) {
      LOGGER.error(COULD_NOT_CLOSE_PAGING_PROVIDER, e);
    }
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
      return new DefaultConnectionSupplier(extensionConnectionSupplier.getConnection(executionContext));
    }

    @Override
    public void dispose() {

    }
  }


  private class StickyConnectionSupplierFactory implements ConnectionSupplierFactory {

    private ConnectionHandler connectionHandler;

    private final LazyValue<ConnectionSupplier> stickyConnection = new LazyValue<>(new CheckedSupplier<ConnectionSupplier>() {

      @Override
      public ConnectionSupplier getChecked() throws Throwable {
        StickyConnectionSupplierFactory.this.connectionHandler = extensionConnectionSupplier.getConnection(executionContext);
        return new StickyConnectionSupplier(StickyConnectionSupplierFactory.this.connectionHandler);
      }
    });

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

    void invalidateConnection();
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

    public void invalidateConnection() {
      connectionHandler.invalidate();
    }
  }


  private class StickyConnectionSupplier implements ConnectionSupplier {

    private final Object connection;
    private final ConnectionHandler connectionHandler;

    public StickyConnectionSupplier(ConnectionHandler connectionHandler) throws ConnectionException {
      this.connectionHandler = connectionHandler;
      this.connection = connectionHandler.getConnection();
    }

    @Override
    public Object getConnection() throws MuleException {
      return connection;
    }

    @Override
    public void close() {

    }

    public void invalidateConnection() {
      connectionHandler.invalidate();
    }
  }
}
