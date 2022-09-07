/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.lang.Boolean.parseBoolean;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.inject.Inject;

/**
 * Implementation of {@link ConnectionManager} which manages connections opened on a specific application.
 *
 * @since 4.1
 */
public final class DelegateConnectionManagerAdapter implements ConnectionManagerAdapter {

  private ConnectionManagerAdapter delegate;
  private ConnectionManagerAdapter connectionManagerAdapterStrategy;

  private MuleContext muleContext;

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
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(connectionManagerAdapterStrategy, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(connectionManagerAdapterStrategy);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(connectionManagerAdapterStrategy);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(connectionManagerAdapterStrategy);
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
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate);
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
      disposeIfNeeded(delegate);
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
      stopIfNeeded(delegate);
    }

    @Override
    public void dispose() {
      disposeIfNeeded(delegate);
    }

    @Override
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate);
    }

    @Override
    public void start() throws MuleException {
      startIfNeeded(delegate);
    }

    @Override
    public PoolingProfile getDefaultPoolingProfile() {
      return delegate.getDefaultPoolingProfile();
    }
  }

  class LazyInvocationHandler implements InvocationHandler {

    private Object config;
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
      return method.invoke(connection, args);
    }
  }

}
