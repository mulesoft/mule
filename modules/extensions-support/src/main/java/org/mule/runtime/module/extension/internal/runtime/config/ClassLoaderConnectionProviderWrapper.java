/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.connection.AbstractConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;

/**
 * An {@link ConnectionProviderWrapper} which makes sure that all delegate methods are executed with a given {@link #classLoader}
 *
 * @param <C> the generic type of the connections the delegate produces
 * @since 4.1.6
 */
public class ClassLoaderConnectionProviderWrapper<C> extends AbstractConnectionProviderWrapper<C> {

  /**
   * Creates a new wrapper for the given {@code provider}
   *
   * @param provider    the delegate
   * @param classLoader the {@link ClassLoader} to use
   * @param <C>         the generic type of the connections the delegate produces
   * @return a new instance
   */
  public static <C> ClassLoaderConnectionProviderWrapper<C> newInstance(ConnectionProvider<C> provider, ClassLoader classLoader) {
    return provider instanceof PoolingConnectionProvider
        ? new PoolingClassLoaderConnectionProviderWrapper<>(provider, classLoader)
        : new ClassLoaderConnectionProviderWrapper<>(provider, classLoader);
  }

  protected final ClassLoader classLoader;

  private ClassLoaderConnectionProviderWrapper(ConnectionProvider<C> provider, ClassLoader classLoader) {
    super(provider);
    this.classLoader = classLoader;
  }

  @Override
  public C connect() throws ConnectionException {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      return getDelegate().connect();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      return getDelegate().validate(connection);
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void disconnect(C connection) {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      getDelegate().disconnect(connection);
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      super.initialise();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void start() throws MuleException {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      super.start();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void stop() throws MuleException {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      super.stop();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void dispose() {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      super.dispose();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof HasReconnectionConfig) {
      return ((HasReconnectionConfig) delegate).getReconnectionConfig();
    }
    return Optional.empty();
  }

  private static class PoolingClassLoaderConnectionProviderWrapper<C> extends ClassLoaderConnectionProviderWrapper<C> implements
      PoolingConnectionProvider<C> {

    private PoolingClassLoaderConnectionProviderWrapper(ConnectionProvider provider, ClassLoader classLoader) {
      super(provider, classLoader);
      checkArgument(provider instanceof PoolingConnectionProvider, "Delegate is not a pooling provider");
    }

    @Override
    public void onBorrow(C connection) {
      Thread thread = Thread.currentThread();
      ClassLoader currentClassLoader = thread.getContextClassLoader();
      setContextClassLoader(thread, currentClassLoader, classLoader);
      try {
        getPoolingDelegate().onBorrow(connection);
      } finally {
        setContextClassLoader(thread, classLoader, currentClassLoader);
      }
    }

    @Override
    public void onReturn(C connection) {
      Thread thread = Thread.currentThread();
      ClassLoader currentClassLoader = thread.getContextClassLoader();
      setContextClassLoader(thread, currentClassLoader, classLoader);
      try {
        getPoolingDelegate().onReturn(connection);
      } finally {
        setContextClassLoader(thread, classLoader, currentClassLoader);
      }
    }

    private PoolingConnectionProvider<C> getPoolingDelegate() {
      return (PoolingConnectionProvider<C>) getDelegate();
    }
  }
}
