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

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link ConnectionProviderWrapper}
 *
 * @param <C> the generic type of the connections that {@link #getDelegate()} produces
 * @since 4.3.0
 */
public abstract class AbstractConnectionProviderWrapper<C> implements ConnectionProviderWrapper<C> {

  private static final Logger LOGGER = getLogger(AbstractConnectionProviderWrapper.class);

  @Inject
  protected MuleContext muleContext;

  private final ConnectionProvider<C> delegate;

  /**
   * Creates a new instance which wraps the {@code delegate}
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   */
  public AbstractConnectionProviderWrapper(ConnectionProvider<C> delegate) {
    this.delegate = delegate;
  }

  @Override
  public C connect() throws ConnectionException {
    return ConnectionUtils.connect(delegate);
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
  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return ConnectionUtils.getRetryPolicyTemplate(getReconnectionConfig());
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
