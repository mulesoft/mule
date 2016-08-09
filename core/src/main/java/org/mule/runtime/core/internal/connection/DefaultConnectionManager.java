/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.util.Reference;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConnectionManager} which manages connections opened on a specific application.
 *
 * @since 4.0
 */
public final class DefaultConnectionManager implements ConnectionManagerAdapter, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectionManager.class);

  private final Map<Reference<Object>, ConnectionManagementStrategy> connections = new HashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final MuleContext muleContext;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final PoolingProfile defaultPoolingProfile;
  private final ConnectionManagementStrategyFactory managementStrategyFactory;

  /**
   * Creates a new instance
   *
   * @param muleContext the {@link MuleContext} of the owned application
   */
  @Inject
  public DefaultConnectionManager(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.defaultPoolingProfile = new PoolingProfile();
    this.retryPolicyTemplate = new NoRetryPolicyTemplate();
    managementStrategyFactory = new ConnectionManagementStrategyFactory(defaultPoolingProfile, muleContext);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if invoked while the {@link #muleContext} is stopped or stopping
   */
  @Override
  public <Config, Connection> void bind(Config owner, ConnectionProvider<Connection> connectionProvider) {
    assertNotStopping(muleContext, "Mule is shutting down... cannot bind new connections");

    connectionProvider = new LifecycleAwareConnectionProviderWrapper<>(connectionProvider, muleContext);
    ConnectionManagementStrategy<Connection> managementStrategy = managementStrategyFactory.getStrategy(connectionProvider);

    ConnectionManagementStrategy<Connection> previous = null;

    writeLock.lock();
    try {
      previous = connections.put(new Reference<>(owner), managementStrategy);
    } finally {
      writeLock.unlock();
    }

    if (previous != null) {
      close(previous);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBinding(Object config) {
    return connections.containsKey(new Reference<>(config));
  }

  /**
   * {@inheritDoc}
   */
  // TODO: MULE-9082
  @Override
  public void unbind(Object config) {
    ConnectionManagementStrategy managementStrategy;
    writeLock.lock();
    try {
      managementStrategy = connections.remove(new Reference<>(config));
    } finally {
      writeLock.unlock();
    }

    if (managementStrategy != null) {
      close(managementStrategy);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <Config, Connection> ConnectionHandler<Connection> getConnection(Config config) throws ConnectionException {
    ConnectionManagementStrategy<Connection> handlingStrategy = null;
    readLock.lock();
    try {
      handlingStrategy = connections.get(new Reference<>(config));
    } finally {
      readLock.unlock();
    }

    if (handlingStrategy == null) {
      throw new ConnectionException("No ConnectionProvider has been registered for owner " + config);
    }

    return handlingStrategy.getConnectionHandler();
  }

  /**
   * Breaks all bindings and closes all connections
   *
   * @throws MuleException in case of error.
   */
  @Override
  public void stop() throws MuleException {
    writeLock.lock();
    try {
      connections.values().stream().forEach(this::close);
      connections.clear();
    } finally {
      writeLock.unlock();
    }
  }

  // TODO: MULE-9082
  private void close(ConnectionManagementStrategy managementStrategy) {
    try {
      managementStrategy.close();
    } catch (Exception e) {
      LOGGER.warn("An error was found trying to release connections", e);
    }
  }

  @Override
  public void dispose() {
    disposeIfNeeded(retryPolicyTemplate, LOGGER);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(retryPolicyTemplate, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(retryPolicyTemplate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RetryPolicyTemplate getDefaultRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PoolingProfile getDefaultPoolingProfile() {
    return defaultPoolingProfile;
  }

}
