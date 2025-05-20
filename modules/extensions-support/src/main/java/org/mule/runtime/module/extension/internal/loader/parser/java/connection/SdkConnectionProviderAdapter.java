/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.connection.adapter.XATransactionalConnectionProvider;
import org.mule.runtime.core.internal.registry.InjectionTargetDecorator;
import org.mule.sdk.api.connectivity.TransactionalConnection;

import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * Adapts a sdk-api {@link org.mule.sdk.api.connectivity.ConnectionProvider} into a mule-api {@link ConnectionProvider}.
 * <p>
 * This adapter acts as a decorator which (if needed) propagates the {@link Lifecycle} and {@link MuleContextAware} signals to the
 * delegate. It also considers the case of the delegate being a pooling or cached provider, in which the returned adapter will
 * also implement the necessary interfaces.
 * <p>
 * Instances are to be created through the {@link #from(Object)} factory method.
 *
 * @param <C> the connection generic type
 * @since 4.5.0
 */
public class SdkConnectionProviderAdapter<C> implements ConnectionProvider<C>, Lifecycle, MuleContextAware,
    InjectionTargetDecorator<org.mule.sdk.api.connectivity.ConnectionProvider<C>> {

  private static final Logger LOGGER = getLogger(SdkConnectionProviderAdapter.class);

  private final org.mule.sdk.api.connectivity.ConnectionProvider<C> delegate;

  @Inject
  private MuleContext muleContext;

  /**
   * Returns the given {@code connectionProvider} as a {@link ConnectionProvider}, creating an adapter around it if needed.
   * <p>
   * If the provider is already an adapter or a native {@link ConnectionProvider}, the same instance is returned. Otherwise, an
   * adapter is created.
   *
   * @param connectionProvider the instance to be adapted
   * @param <C>                the connection's generic type
   * @return a {@link ConnectionProvider}
   * @throws IllegalArgumentException if {@code connectionProvider} is {@code null} or not a valid connection provider.
   */
  public static <C> ConnectionProvider<C> from(Object connectionProvider) {
    if (connectionProvider != null) {
      if (connectionProvider instanceof ConnectionProvider) {
        return (ConnectionProvider<C>) connectionProvider;
      } else if (connectionProvider instanceof org.mule.sdk.api.connectivity.CachedConnectionProvider cachedConnectionProvider) {
        return fromCachedManagement(cachedConnectionProvider);
      } else if (connectionProvider instanceof org.mule.sdk.api.connectivity.PoolingConnectionProvider poolingConnectionProvider) {
        return fromPooledManagement(poolingConnectionProvider);
      } else if (connectionProvider instanceof org.mule.sdk.api.connectivity.ConnectionProvider nullMgmtConnectionProvider) {
        return fromNullManagement(nullMgmtConnectionProvider);
      } else {
        throw new IllegalArgumentException("Unsupported ConnectionProvider type " + connectionProvider.getClass().getName());
      }
    } else {
      throw new IllegalArgumentException("connectionProvider cannot be null");
    }
  }

  private static <C> ConnectionProvider<C> fromCachedManagement(org.mule.sdk.api.connectivity.CachedConnectionProvider connectionProvider) {
    if (connectionProvider instanceof org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) {
      return new SdkCachedXATransactionalConnectionProviderAdapter<>(connectionProvider);
    } else {
      return new SdkCachedConnectionProviderAdapter<>(connectionProvider);
    }
  }

  private static <C> ConnectionProvider<C> fromPooledManagement(org.mule.sdk.api.connectivity.PoolingConnectionProvider connectionProvider) {
    if (connectionProvider instanceof org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) {
      return new SdkPoolingXATransactionalConnectionProviderAdapter<>(connectionProvider);
    } else {
      return new SdkPoolingConnectionProviderAdapter<>(connectionProvider);
    }
  }

  private static <C> ConnectionProvider<C> fromNullManagement(org.mule.sdk.api.connectivity.ConnectionProvider connectionProvider) {
    if (connectionProvider instanceof org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) {
      return new SdkXATransactionalConnectionProviderAdapter<>(connectionProvider);
    } else {
      return new SdkConnectionProviderAdapter<>(connectionProvider);
    }
  }

  private SdkConnectionProviderAdapter(org.mule.sdk.api.connectivity.ConnectionProvider<C> delegate) {
    this.delegate = delegate;
  }

  @Override
  public C connect() throws ConnectionException {
    return delegate.connect();
  }

  @Override
  public void disconnect(C connection) {
    delegate.disconnect(connection);
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    return new SdkConnectionValidationResultAdapter(delegate.validate(connection));
  }

  @Override
  public org.mule.sdk.api.connectivity.ConnectionProvider<C> getDelegate() {
    return delegate;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (muleContext != null) {
      initialiseIfNeeded(delegate, true, muleContext);
    } else {
      initialiseIfNeeded(delegate);
    }
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(delegate);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(delegate);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
    if (delegate instanceof MuleContextAware) {
      ((MuleContextAware) delegate).setMuleContext(muleContext);
    }
  }

  private static class SdkXATransactionalConnectionProviderAdapter<C extends TransactionalConnection>
      extends SdkConnectionProviderAdapter<C>
      implements XATransactionalConnectionProvider<C> {

    private SdkXATransactionalConnectionProviderAdapter(org.mule.sdk.api.connectivity.ConnectionProvider<C> delegate) {
      super(delegate);
    }

    @Override
    public PoolingProfile getXaPoolingProfile() {
      return ((org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) getDelegate()).getXaPoolingProfile();
    }
  }

  private static class SdkPoolingConnectionProviderAdapter<C> extends SdkConnectionProviderAdapter<C>
      implements PoolingConnectionProvider<C> {

    private final org.mule.sdk.api.connectivity.PoolingConnectionProvider<C> delegate;

    private SdkPoolingConnectionProviderAdapter(org.mule.sdk.api.connectivity.PoolingConnectionProvider<C> delegate) {
      super(delegate);
      this.delegate = delegate;
    }

    @Override
    public void onBorrow(C connection) {
      delegate.onBorrow(connection);
    }

    @Override
    public void onReturn(C connection) {
      delegate.onReturn(connection);
    }
  }

  private static class SdkPoolingXATransactionalConnectionProviderAdapter<C extends TransactionalConnection>
      extends SdkPoolingConnectionProviderAdapter<C>
      implements XATransactionalConnectionProvider<C> {

    private SdkPoolingXATransactionalConnectionProviderAdapter(org.mule.sdk.api.connectivity.PoolingConnectionProvider<C> delegate) {
      super(delegate);
    }

    @Override
    public PoolingProfile getXaPoolingProfile() {
      return ((org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) getDelegate()).getXaPoolingProfile();
    }
  }

  private static class SdkCachedConnectionProviderAdapter<C> extends SdkConnectionProviderAdapter<C>
      implements CachedConnectionProvider<C> {

    private SdkCachedConnectionProviderAdapter(org.mule.sdk.api.connectivity.CachedConnectionProvider<C> delegate) {
      super(delegate);
    }
  }

  private static class SdkCachedXATransactionalConnectionProviderAdapter<C extends TransactionalConnection>
      extends SdkCachedConnectionProviderAdapter<C>
      implements XATransactionalConnectionProvider<C> {

    private SdkCachedXATransactionalConnectionProviderAdapter(org.mule.sdk.api.connectivity.CachedConnectionProvider<C> delegate) {
      super(delegate);
    }

    @Override
    public PoolingProfile getXaPoolingProfile() {
      return ((org.mule.sdk.api.connectivity.XATransactionalConnectionProvider) getDelegate()).getXaPoolingProfile();
    }
  }
}
