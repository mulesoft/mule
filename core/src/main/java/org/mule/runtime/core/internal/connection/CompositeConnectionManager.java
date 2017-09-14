/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import org.slf4j.Logger;

/**
 * {@link ConnectionManager} implementation which composes two {@link ConnectionManager} in a hierarchy manner.
 * <p>
 * If the this manager can't process a request with the {@link CompositeConnectionManager#childConnectionManager},
 * it will fallback to the {@link CompositeConnectionManager#parentConnectionManager}
 *
 * @since 4.0
 */
public class CompositeConnectionManager implements ConnectionManager, Lifecycle, ConnectionManagerAdapter {

  private static final Logger LOGGER = getLogger(CompositeConnectionManager.class);

  private final ConnectionManagerAdapter childConnectionManager;
  private final ConnectionManagerAdapter parentConnectionManager;

  /**
   * Creates a new instance of {@link CompositeConnectionManager}
   *
   * @param childConnectionManager  {@link ConnectionManager} considered as the main one
   * @param parentConnectionManager {@link ConnectionManager} considered as the secondary one, is a request
   *                                can't be handled by the main {@link ConnectionManager}, this one will be used
   */
  public CompositeConnectionManager(ConnectionManagerAdapter childConnectionManager,
                                    ConnectionManagerAdapter parentConnectionManager) {
    checkNotNull(childConnectionManager, "'childConnectionManager' can't be null");
    checkNotNull(parentConnectionManager, "'parentConnectionManager' can't be null");

    this.childConnectionManager = childConnectionManager;
    this.parentConnectionManager = parentConnectionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> void bind(Object config, ConnectionProvider<C> connectionProvider) {
    childConnectionManager.bind(config, connectionProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBinding(Object config) {
    return childConnectionManager.hasBinding(config) || parentConnectionManager.hasBinding(config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unbind(Object config) {
    if (childConnectionManager.hasBinding(config)) {
      childConnectionManager.unbind(config);
    } else if (parentConnectionManager.hasBinding(config)) {
      parentConnectionManager.unbind(config);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
    return childConnectionManager.hasBinding(config) ? childConnectionManager.getConnection(config)
        : parentConnectionManager.getConnection(config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
    return childConnectionManager.testConnectivity(connectionProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
      throws IllegalArgumentException {
    Object value = configurationInstance.getValue();

    if (childConnectionManager.hasBinding(value)) {
      return childConnectionManager.testConnectivity(configurationInstance);
    } else {
      return parentConnectionManager.testConnectivity(configurationInstance);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
    return childConnectionManager.testConnectivity(connection, connectionHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
    return childConnectionManager.getRetryTemplateFor(connectionProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
    return childConnectionManager.getReconnectionConfigFor(connectionProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PoolingProfile getDefaultPoolingProfile() {
    return childConnectionManager.getDefaultPoolingProfile();
  }

  @Override
  public void dispose() {
    disposeIfNeeded(childConnectionManager, LOGGER);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(childConnectionManager);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(childConnectionManager);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(childConnectionManager);
  }
}
