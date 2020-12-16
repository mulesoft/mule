/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.streaming;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.COMPONENT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.IS_TRANSACTIONAL;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.refreshTokenIfNecessary;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMutableConfigurationStats;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.NULL_THROWABLE_CONSUMER;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.isPartOfActiveTransaction;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.shouldRetry;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * Implementation of {@link Producer} that uses an instance of {@link PagingProvider} to get its results.
 *
 * @param <T> the type of the elements returned by the {@link PagingProvider}.
 * @since 3.5.0
 */
public final class PagingProviderProducer<T> implements Producer<List<T>> {

  private static final Logger LOGGER = getLogger(PagingProviderProducer.class);

  public static final String COULD_NOT_OBTAIN_A_CONNECTION = "Could not obtain a connection for the configuration";
  public static final String COULD_NOT_CREATE_A_CONNECTION_SUPPLIER =
      "Could not obtain a connection supplier for the configuration";
  public static final String COULD_NOT_EXECUTE = "Could not execute operation with connection";
  private PagingProvider<Object, T> delegate;
  private final ConfigurationInstance config;
  private final ExtensionConnectionSupplier extensionConnectionSupplier;
  private final ExecutionContextAdapter executionContext;
  private final ConnectionSupplierFactory connectionSupplierFactory;
  private final RetryPolicyTemplate retryPolicy;
  private final boolean supportsOAuth;
  private boolean isFirstPage = true;
  private AtomicBoolean alreadyClosed = new AtomicBoolean(false);
  private final MutableConfigurationStats mutableStats;

  public PagingProviderProducer(PagingProvider<Object, T> delegate,
                                ConfigurationInstance config,
                                ExecutionContextAdapter executionContext,
                                ExtensionConnectionSupplier extensionConnectionSupplier) {
    this(delegate, config, executionContext, extensionConnectionSupplier, false);
  }

  public PagingProviderProducer(PagingProvider<Object, T> delegate,
                                ConfigurationInstance config,
                                ExecutionContextAdapter executionContext,
                                ExtensionConnectionSupplier extensionConnectionSupplier,
                                boolean supportsOAuth) {
    this.delegate = new PagingProviderWrapper(delegate, executionContext.getExtensionModel());
    this.config = config;
    this.executionContext = executionContext;
    this.extensionConnectionSupplier = extensionConnectionSupplier;
    this.supportsOAuth = supportsOAuth;
    retryPolicy = (RetryPolicyTemplate) executionContext.getRetryPolicyTemplate().orElseGet(NoRetryPolicyTemplate::new);
    connectionSupplierFactory = createConnectionSupplierFactory();
    mutableStats = getMutableConfigurationStats(executionContext);
  }

  /**
   * Asks the delegate for the next page
   */
  @Override
  public List<T> produce() {
    List<T> page = performWithConnection(delegate::getPage);
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
    if (retryPolicy.isEnabled()) {
      CompletableFuture<R> future = retryPolicy.applyPolicy(() -> completedFuture(withConnection(function, supportsOAuth)),
                                                            e -> !isFirstPage && !delegate.useStickyConnections()
                                                                && shouldRetry(e, executionContext),
                                                            NULL_THROWABLE_CONSUMER,
                                                            NULL_THROWABLE_CONSUMER,
                                                            identity(),
                                                            executionContext.getCurrentScheduler());
      try {
        return future.get();
      } catch (ExecutionException e) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        }
        throw new MuleRuntimeException(createStaticMessage(COULD_NOT_EXECUTE), e.getCause());
      } catch (InterruptedException e) {
        throw new MuleRuntimeException(createStaticMessage(COULD_NOT_EXECUTE), e);
      }
    } else {
      return withConnection(function, supportsOAuth);
    }
  }

  private <R> R withConnection(Function<Object, R> function, boolean refreshOAuth) {
    ConnectionSupplier connectionSupplier = getConnectionSupplier();
    Object connection = getConnection(connectionSupplier);
    try {
      return function.apply(connection);
    } catch (Exception caughtException) {
      if (isFirstPage) {
        safely(() -> delegate.close(connection), e -> LOGGER.error("Found exception closing paging provider", e));
      } else if (refreshOAuth) {
        boolean tokenRefreshed;
        try {
          tokenRefreshed = refreshTokenIfNecessary(executionContext, caughtException);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }

        if (tokenRefreshed) {
          return withConnection(function, false);
        }
      }

      handleException(caughtException, connectionSupplier);
      throw caughtException;
    } finally {
      safely(connectionSupplier::close, e -> LOGGER.error("Found exception closing the connection supplier", e));
    }
  }

  private void handleException(Exception exception, ConnectionSupplier connectionSupplier) {
    ConnectionException connectionException = extractConnectionException(exception).orElse(null);
    if (connectionException != null) {
      if (isPartOfActiveTransaction(config)) {
        connectionException.addInfo(IS_TRANSACTIONAL, true);
      }
      connectionException.addInfo(COMPONENT_CONFIG_NAME, config.getName());
      connectionSupplier.invalidateConnection();
    }
  }

  /**
   * Closes the delegate
   */
  @Override
  public void close() {
    ConnectionSupplier connectionSupplier = null;
    try {
      connectionSupplier = connectionSupplierFactory.getConnectionSupplier();
      delegate.close(connectionSupplier.getConnection());
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(COULD_NOT_OBTAIN_A_CONNECTION), e);
    } finally {
      if (connectionSupplier != null) {
        safely(connectionSupplier::close, e -> LOGGER.debug("Found exception closing the connection supplier", e));
      }
      if (mutableStats != null && alreadyClosed.compareAndSet(false, true)) {
        mutableStats.discountActiveComponent();
      }
      connectionSupplierFactory.dispose();
    }
  }

  private ConnectionSupplierFactory createConnectionSupplierFactory() {
    if (delegate.useStickyConnections()) {
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
