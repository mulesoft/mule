/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

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
import org.mule.runtime.core.internal.registry.InjectionTargetDecorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdkConnectionProviderAdapter<C> implements ConnectionProvider<C>, Lifecycle, MuleContextAware,
    InjectionTargetDecorator<org.mule.sdk.api.connectivity.ConnectionProvider<C>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SdkConnectionProviderAdapter.class);

  private final org.mule.sdk.api.connectivity.ConnectionProvider<C> delegate;
  private MuleContext muleContext;

  public static <C> ConnectionProvider<C> from(Object connectionProvider) {
    if (connectionProvider != null) {
      if (connectionProvider instanceof ConnectionProvider) {
        return (ConnectionProvider<C>) connectionProvider;
      } else if (connectionProvider instanceof org.mule.sdk.api.connectivity.CachedConnectionProvider) {
        return new SdkCachedConnectionProviderAdapter<>((org.mule.sdk.api.connectivity.CachedConnectionProvider<C>) connectionProvider);
      } else if (connectionProvider instanceof org.mule.sdk.api.connectivity.PoolingConnectionProvider) {
        return new SdkPoolingConnectionProviderAdapter<>((org.mule.sdk.api.connectivity.PoolingConnectionProvider<C>) connectionProvider);
      } else {
        throw new IllegalArgumentException("Unsupported ConnectionProvider type " + connectionProvider.getClass().getName());
      }
    } else {
      throw new IllegalArgumentException("connectionProvider cannot be null");
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

  private static class SdkCachedConnectionProviderAdapter<C> extends SdkConnectionProviderAdapter<C>
      implements CachedConnectionProvider<C> {

    private SdkCachedConnectionProviderAdapter(org.mule.sdk.api.connectivity.CachedConnectionProvider<C> delegate) {
      super(delegate);
    }
  }
}
