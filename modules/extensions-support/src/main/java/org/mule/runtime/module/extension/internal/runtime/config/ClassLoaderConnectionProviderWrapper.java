/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.HasReconnectionConfig;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A {@link ConnectionProviderWrapper} which makes sure that all delegate methods are executed with a given {@link #classLoader}
 *
 * @param <C> the generic type of the connections the delegate produces
 * @since 4.1.6
 */
public class ClassLoaderConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

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

  private final ClassLoader classLoader;

  private ClassLoaderConnectionProviderWrapper(ConnectionProvider<C> provider, ClassLoader classLoader) {
    super(provider);
    this.classLoader = classLoader;
  }

  @Override
  public C connect() throws ConnectionException {
    return onClassLoader(() -> getDelegate().connect(), ConnectionException.class);
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    return onClassLoader(() -> getDelegate().validate(connection));
  }

  @Override
  public void disconnect(C connection) {
    onClassLoader(() -> getDelegate().disconnect(connection));
  }

  @Override
  public void initialise() throws InitialisationException {
    onClassLoader(super::initialise, InitialisationException.class);
  }

  @Override
  public void start() throws MuleException {
    onClassLoader(super::start, MuleException.class);
  }

  @Override
  public void stop() throws MuleException {
    onClassLoader(super::stop, MuleException.class);
  }

  @Override
  public void dispose() {
    onClassLoader(super::dispose);
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    ConnectionProvider<C> delegate = getDelegate();
    if (delegate instanceof HasReconnectionConfig) {
      return ((HasReconnectionConfig) delegate).getReconnectionConfig();
    }
    return Optional.empty();
  }

  protected void onClassLoader(CheckedRunnable runnable) {
    onClassLoader(runnable, RuntimeException.class);
  }

  private <T> T onClassLoader(Callable<T> callable) {
    return onClassLoader(callable, RuntimeException.class);
  }

  private <E extends Exception> void onClassLoader(CheckedRunnable runnable, Class<E> expectedException) throws E {
    onClassLoader(() -> {
      runnable.run();
      return null;
    }, expectedException);
  }

  private <T, E extends Exception> T onClassLoader(Callable<T> callable, Class<E> expectedException) throws E {
    return withContextClassLoader(classLoader, callable, expectedException, e -> {
      throw new MuleRuntimeException(e);
    });
  }

  private static class PoolingClassLoaderConnectionProviderWrapper<C> extends ClassLoaderConnectionProviderWrapper<C> implements
      PoolingConnectionProvider<C> {

    private PoolingClassLoaderConnectionProviderWrapper(ConnectionProvider provider, ClassLoader classLoader) {
      super(provider, classLoader);
      checkArgument(provider instanceof PoolingConnectionProvider, "Delegate is not a pooling provider");
    }

    @Override
    public void onBorrow(C connection) {
      onClassLoader(() -> getPoolingDelegate().onBorrow(connection));
    }

    @Override
    public void onReturn(C connection) {
      onClassLoader(() -> getPoolingDelegate().onReturn(connection));
    }

    private PoolingConnectionProvider<C> getPoolingDelegate() {
      return (PoolingConnectionProvider<C>) getDelegate();
    }
  }
}
