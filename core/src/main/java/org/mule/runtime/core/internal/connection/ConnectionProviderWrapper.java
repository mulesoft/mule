/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.util.Optional.empty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.HasPoolingProfile;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import javax.inject.Inject;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * Base class for wrappers for {@link ConnectionProvider} instances
 *
 * @param <C> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
public abstract class ConnectionProviderWrapper<C>
    implements ConnectionProvider<C>, HasPoolingProfile, HasReconnectionConfig, HasDelegate<C>, Lifecycle {

  private static final Logger LOGGER = getLogger(ConnectionProviderWrapper.class);

  @Inject
  protected MuleContext muleContext;

  private final ConnectionProvider<C> delegate;

  /**
   * Creates a new instance which wraps the {@code delegate}
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   */
  ConnectionProviderWrapper(ConnectionProvider<C> delegate) {
    this.delegate = delegate;
  }

  @Override
  public C connect() throws ConnectionException {
    try {
      return delegate.connect();
    } catch (ConnectionException ce) {
      throw ce;
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }

  /**
   * Delegates the connection validation to the delegated {@link ConnectionProvider}
   *
   * @param connection a non {@code null} {@link C}.
   * @return the {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
   */
  @Override
  public ConnectionValidationResult validate(C connection) {
    return delegate.validate(connection);
  }

  @Override
  public void disconnect(C connection) {
    delegate.disconnect(connection);
  }

  @Override
  public ConnectionProvider<C> getDelegate() {
    return delegate;
  }

  /**
   * @return a {@link RetryPolicyTemplate}
   */
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return getReconnectionConfig()
        .map(ReconnectionConfig::getRetryPolicyTemplate)
        .orElseGet(NoRetryPolicyTemplate::new);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
    initialiseIfNeeded(getRetryPolicyTemplate(), true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(delegate);
    startIfNeeded(getRetryPolicyTemplate());
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(delegate);
    stopIfNeeded(getRetryPolicyTemplate());
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
    disposeIfNeeded(getRetryPolicyTemplate(), LOGGER);
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return empty();
  }
}
