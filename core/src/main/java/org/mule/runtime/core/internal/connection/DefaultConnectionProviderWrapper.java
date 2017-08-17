/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.of;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

/**
 * A {@link ConnectionProviderWrapper} which performs base tasks as handling reconnection strategies, DI, etc.
 *
 * @param <C> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
public class DefaultConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

  private final MuleContext muleContext;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param muleContext the owning {@link MuleContext}
   */
  public DefaultConnectionProviderWrapper(ConnectionProvider<C> delegate, MuleContext muleContext) {
    super(delegate);
    this.muleContext = muleContext;
  }

  /**
   * Obtains a {@code Connection} from the delegate and applies injection dependency and the {@code muleContext}'s completed
   * {@link Lifecycle} phases
   *
   * @return a {@code Connection} with dependencies injected and the correct lifecycle state
   * @throws ConnectionException if an exception was found obtaining the connection or managing it
   */
  @Override
  public C connect() throws ConnectionException {
    C connection = super.connect();
    try {
      muleContext.getInjector().inject(connection);
    } catch (MuleException e) {
      throw new ConnectionException("Could not initialise connection", e);
    }

    return connection;
  }

  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    final ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof ConnectionProviderWrapper) {
      return ((ConnectionProviderWrapper) delegate).getRetryPolicyTemplate();
    }

    return super.getRetryPolicyTemplate();
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    final ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof ConnectionProviderWrapper) {
      return ((ConnectionProviderWrapper) delegate).getReconnectionConfig();
    }

    return of(ReconnectionConfig.getDefault());
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    ConnectionProvider<C> delegate = getDelegate();
    return delegate instanceof ConnectionProviderWrapper ? ((ConnectionProviderWrapper) delegate).getPoolingProfile()
        : Optional.empty();
  }
}
