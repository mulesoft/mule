/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * A {@link ConnectionProviderWrapper} which performs lifecycle and dependency injection operations on the generated connections
 *
 * @param <C> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
public class LifecycleAwareConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

  private static final Logger LOGGER = getLogger(LifecycleAwareConnectionProviderWrapper.class);

  private final MuleContext muleContext;
  private final ConnectionManagerAdapter connectionManager;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ConnectionProvider} to be wrapped
   * @param muleContext the owning {@link MuleContext}
   */
  public LifecycleAwareConnectionProviderWrapper(ConnectionProvider<C> delegate, MuleContext muleContext) {
    super(delegate);
    this.muleContext = muleContext;
    connectionManager = muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER);
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
      muleContext.getRegistry().applyLifecycle(connection, NotInLifecyclePhase.PHASE_NAME, Startable.PHASE_NAME);
    } catch (MuleException e) {
      throw new ConnectionException("Could not initialise connection", e);
    }

    return connection;
  }

  /**
   * Disconnects the {@code connection} and then applies all the necessary {@link Lifecycle} phases until the
   * {@link Disposable#PHASE_NAME} transition is reached
   *
   * @param connection the {@code Connection} to be destroyed
   */
  @Override
  public void disconnect(C connection) {
    try {
      super.disconnect(connection);
    } finally {
      try {
        muleContext.getRegistry().applyLifecycle(connection, Startable.PHASE_NAME, Disposable.PHASE_NAME);
      } catch (MuleException e) {
        LOGGER.warn("Exception was found trying to dispose connection", e);
      }
    }
  }

  /**
   * @return a {@link RetryPolicyTemplate} from the delegated {@link ConnectionProviderWrapper}, if the {@link #delegate} is a
   *         {@link ConnectionProvider} then a default {@link RetryPolicy} is returned from the {@link ConnectionProviderWrapper}
   */
  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    final ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof ConnectionProviderWrapper) {
      return ((ConnectionProviderWrapper) delegate).getRetryPolicyTemplate();
    }

    return connectionManager.getDefaultRetryPolicyTemplate();
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    ConnectionProvider<C> delegate = getDelegate();
    return delegate instanceof ConnectionProviderWrapper ? ((ConnectionProviderWrapper) delegate).getPoolingProfile()
        : Optional.empty();
  }
}
