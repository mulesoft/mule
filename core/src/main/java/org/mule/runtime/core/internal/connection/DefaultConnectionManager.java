/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Implementation of {@link ConnectionManager} which manages connections opened on a specific application.
 *
 * @since 4.0
 */
public final class DefaultConnectionManager implements ConnectionManagerAdapter, Lifecycle {

  private static final Logger LOGGER = getLogger(DefaultConnectionManager.class);

  private final Map<Reference<Object>, ConnectionManagementStrategy> connections = new HashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final MuleContext muleContext;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final PoolingProfile defaultPoolingProfile;
  private final ConnectionManagementStrategyFactory managementStrategyFactory;
  private final ReconnectionConfig defaultReconnectionConfig = ReconnectionConfig.getDefault();

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
  public <C> void bind(Object owner, ConnectionProvider<C> connectionProvider) {
    assertNotStopping(muleContext, "Mule is shutting down... cannot bind new connections");

    connectionProvider = new DefaultConnectionProviderWrapper<>(connectionProvider, muleContext);
    ConnectionManagementStrategy<C> managementStrategy = managementStrategyFactory.getStrategy(connectionProvider);

    ConnectionManagementStrategy<C> previous;

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
  public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
    return connectionProvider instanceof ConnectionProviderWrapper
        ? ((ConnectionProviderWrapper) connectionProvider).getRetryPolicyTemplate()
        : defaultReconnectionConfig.getRetryPolicyTemplate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
    return connectionProvider instanceof ConnectionProviderWrapper
        ? ((ConnectionProviderWrapper) connectionProvider).getReconnectionConfig().orElse(defaultReconnectionConfig)
        : defaultReconnectionConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
    return doTestConnectivity(() -> doTestConnectivity(connectionProvider,
                                                       managementStrategyFactory.getStrategy(connectionProvider)
                                                           .getConnectionHandler()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
    if (!(connectionHandler instanceof ConnectionHandlerAdapter)) {
      throw new IllegalArgumentException("ConnectionHandler was not produced through this manager");
    }

    return ((ConnectionHandlerAdapter) connectionHandler).getConnectionProvider().validate(connection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
      throws IllegalArgumentException {

    if (!configurationInstance.getConnectionProvider().isPresent()) {
      throw new IllegalArgumentException("The component does not support connectivity testing");
    }

    return doTestConnectivity(() -> {
      ConnectionProvider<Object> connectionProvider = configurationInstance.getConnectionProvider().get();
      final Object config = configurationInstance.getValue();
      ConnectionHandler<Object> connectionHandler;
      try {
        readLock.lock();
        try {
          connectionHandler = hasBinding(config)
              ? getConnection(config)
              : managementStrategyFactory.getStrategy(connectionProvider).getConnectionHandler();
        } finally {
          readLock.unlock();
        }
      } catch (ConnectionException e) {
        return failure(e.getMessage(), e.getErrorType().orElse(null), e);
      }

      return doTestConnectivity(connectionProvider, connectionHandler);
    });

  }

  private ConnectionValidationResult doTestConnectivity(Callable<ConnectionValidationResult> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      return failure("Exception was found trying to test connectivity", e);
    }
  }

  private <C> ConnectionValidationResult doTestConnectivity(ConnectionProvider<C> connectionProvider,
                                                            ConnectionHandler<C> connectionHandler)
      throws Exception {
    try {
      return connectionProvider.validate(connectionHandler.getConnection());
    } catch (ConnectionException e) {
      return failure(e.getMessage(), e.getErrorType().orElse(null), e);
    } finally {
      if (connectionHandler != null) {
        connectionHandler.release();
      }
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
  public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
    ConnectionManagementStrategy<C> handlingStrategy = null;
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
  public PoolingProfile getDefaultPoolingProfile() {
    return defaultPoolingProfile;
  }

}
