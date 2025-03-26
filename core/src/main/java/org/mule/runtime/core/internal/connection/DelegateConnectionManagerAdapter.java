/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.lang.Boolean.parseBoolean;
import static java.lang.reflect.Proxy.newProxyInstance;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jakarta.inject.Inject;

/**
 * Implementation of {@link ConnectionManager} which manages connections opened on a specific application.
 *
 * @since 4.1
 */
public final class DelegateConnectionManagerAdapter implements ConnectionManagerAdapter {

  private final MuleContext muleContext;
  private final ConnectionManagerAdapter delegate;
  private ConnectionManagerAdapter connectionManagerAdapterStrategy;

  @Inject
  public DelegateConnectionManagerAdapter(MuleContext muleContext) {
    this.muleContext = muleContext;
    delegate = new DefaultConnectionManager(muleContext);
    if (parseBoolean(muleContext.getDeploymentProperties().getProperty(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, "false"))) {
      connectionManagerAdapterStrategy = new LazyConnectionManagerAdapter();
    } else {
      connectionManagerAdapterStrategy = new EagerConnectionManagerAdapter();
    }
  }

  @Override
  public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
    return connectionManagerAdapterStrategy.getRetryTemplateFor(connectionProvider);
  }

  @Override
  public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
    return connectionManagerAdapterStrategy.getReconnectionConfigFor(connectionProvider);
  }

  @Override
  public PoolingProfile getDefaultPoolingProfile() {
    return connectionManagerAdapterStrategy.getDefaultPoolingProfile();
  }

  @Override
  public <C> void bind(Object config, ConnectionProvider<C> connectionProvider) {
    connectionManagerAdapterStrategy.bind(config, connectionProvider);
  }

  @Override
  public boolean hasBinding(Object config) {
    return connectionManagerAdapterStrategy.hasBinding(config);
  }

  @Override
  public void unbind(Object config) {
    connectionManagerAdapterStrategy.unbind(config);
  }

  @Override
  public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
    return connectionManagerAdapterStrategy.getConnection(config);
  }

  @Override
  public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
    return connectionManagerAdapterStrategy.testConnectivity(connectionProvider);
  }

  @Override
  public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
    return connectionManagerAdapterStrategy.testConnectivity(connection, connectionHandler);
  }

  @Override
  public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
      throws IllegalArgumentException {
    return connectionManagerAdapterStrategy.testConnectivity(configurationInstance);
  }

  @Override
  public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance, boolean force)
      throws IllegalArgumentException {
    return connectionManagerAdapterStrategy.testConnectivity(configurationInstance, force);
  }

  @Override
  public void initialise() throws InitialisationException {
    connectionManagerAdapterStrategy.initialise();
  }

  @Override
  public void start() throws MuleException {
    connectionManagerAdapterStrategy.start();
  }

  @Override
  public void stop() throws MuleException {
    connectionManagerAdapterStrategy.stop();
  }

  @Override
  public void dispose() {
    connectionManagerAdapterStrategy.dispose();
  }

  /**
   * {@link ConnectionManagerAdapter} implementation commonly used for running applications in production.
   *
   * @since 4.1
   */
  class EagerConnectionManagerAdapter implements ConnectionManagerAdapter {

    @Override
    public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
      return delegate.getRetryTemplateFor(connectionProvider);
    }

    @Override
    public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
      return delegate.getReconnectionConfigFor(connectionProvider);
    }

    @Override
    public PoolingProfile getDefaultPoolingProfile() {
      return delegate.getDefaultPoolingProfile();
    }

    @Override
    public <C> void bind(Object config, ConnectionProvider<C> connectionProvider) {
      delegate.bind(config, connectionProvider);
    }

    @Override
    public boolean hasBinding(Object config) {
      return delegate.hasBinding(config);
    }

    @Override
    public void unbind(Object config) {
      delegate.unbind(config);
    }

    @Override
    public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
      return delegate.getConnection(config);
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
      return delegate.testConnectivity(connectionProvider);
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
      return delegate.testConnectivity(connection, connectionHandler);
    }

    @Override
    public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
        throws IllegalArgumentException {
      return delegate.testConnectivity(configurationInstance);
    }

    @Override
    public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance, boolean force)
        throws IllegalArgumentException {
      return delegate.testConnectivity(configurationInstance, force);
    }

    @Override
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate, true, muleContext);
    }

    @Override
    public void start() throws MuleException {
      delegate.start();
    }

    @Override
    public void stop() throws MuleException {
      delegate.stop();
    }

    @Override
    public void dispose() {
      delegate.dispose();
    }
  }

  /**
   * {@link ConnectionManagerAdapter} implementation that won't create a connection until is not used. The
   * {@link #getConnection(Object)} won't establish the connection since the method won't be invoke until the returned object
   * {@link ConnectionHandler#getConnection()} method is invoked.
   *
   * @since 4.1
   */
  class LazyConnectionManagerAdapter implements ConnectionManagerAdapter {

    @Override
    public <C> void bind(Object owner, ConnectionProvider<C> connectionProvider) {
      delegate.bind(owner, connectionProvider);
    }

    @Override
    public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
      return delegate.getRetryTemplateFor(connectionProvider);
    }

    @Override
    public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
      return delegate.getReconnectionConfigFor(connectionProvider);
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
      return ConnectionValidationResult.success();
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
      return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
        throws IllegalArgumentException {
      return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance, boolean force)
        throws IllegalArgumentException {
      if (force) {
        return delegate.testConnectivity(configurationInstance, force);
      } else {
        return ConnectionValidationResult.success();
      }
    }

    @Override
    public boolean hasBinding(Object config) {
      return delegate.hasBinding(config);
    }

    @Override
    public void unbind(Object config) {
      delegate.unbind(config);
    }

    @Override
    public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
      Object proxyInstance =
          newProxyInstance(config.getClass().getClassLoader(),
                           new Class[] {ConnectionHandler.class, ConnectionHandlerAdapter.class},
                           new LazyInvocationHandler(config));
      return (ConnectionHandler<C>) proxyInstance;
    }

    @Override
    public void stop() throws MuleException {
      delegate.stop();
    }

    @Override
    public void dispose() {
      delegate.dispose();
    }

    @Override
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate, true, muleContext);
    }

    @Override
    public void start() throws MuleException {
      delegate.start();
    }

    @Override
    public PoolingProfile getDefaultPoolingProfile() {
      return delegate.getDefaultPoolingProfile();
    }
  }

  class LazyInvocationHandler implements InvocationHandler {

    private final Object config;
    private ConnectionHandler<Object> connection;

    public LazyInvocationHandler(Object config) {
      this.config = config;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getConnection")) {
        if (connection == null) {
          connection = delegate.getConnection(config);
        }
        return connection.getConnection();
      }

      // Avoid NPE when doing release or invalidate if connection wasn't obtained (for instance, because of an exception on
      // delegate.getConnection())
      if (connection != null) {
        return method.invoke(connection, args);
      } else {
        return null;
      }
    }
  }

}
