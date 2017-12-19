/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

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
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Implementation of {@link ConnectionManager} which manages connections opened on a specific application.
 *
 * @since 4.0
 */
public final class LazyConnectionManager implements ConnectionManagerAdapter, Lifecycle {

  private DefaultConnectionManager defaultConnectionManager;

  @Override
  public <C> void bind(Object owner, ConnectionProvider<C> connectionProvider) {
    defaultConnectionManager.bind(owner, connectionProvider);
  }

  @Override
  public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
    return defaultConnectionManager.getRetryTemplateFor(connectionProvider);
  }

  @Override
  public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
    return defaultConnectionManager.getReconnectionConfigFor(connectionProvider);
  }

  @Override
  public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
    // TODO see if this makes sense
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
    return defaultConnectionManager.hasBinding(config);
  }

  @Override
  public void unbind(Object config) {
    defaultConnectionManager.unbind(config);
  }

  @Override
  public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
    Object proxyInstance =
        Proxy.newProxyInstance(config.getClass().getClassLoader(), ClassUtils.findImplementedInterfaces(config.getClass()),
                               new LazyInvocationHandler(config));
    return defaultConnectionManager.getConnection(config);
  }

  @Override
  public void stop() throws MuleException {
    defaultConnectionManager.stop();
  }

  @Override
  public void dispose() {
    defaultConnectionManager.dispose();
  }

  @Override
  public void initialise() throws InitialisationException {
    defaultConnectionManager.initialise();
  }

  @Override
  public void start() throws MuleException {
    defaultConnectionManager.start();
  }

  @Override
  public PoolingProfile getDefaultPoolingProfile() {
    return defaultConnectionManager.getDefaultPoolingProfile();
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
        connection = defaultConnectionManager.getConnection(config);
        return connection.getConnection();
      }
      return method.invoke(connection, args);
    }
  }
}
