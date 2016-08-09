/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.api.config.ConfigurationInstanceNotification.CONFIGURATION_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationInstanceNotification;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.introspection.Interceptable;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.internal.introspection.AbstractInterceptable;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConfigurationInstance} which propagates dependency injection and lifecycle phases into the contained
 * configuration {@link #value} and {@link #connectionProvider} (if present).
 * <p>
 * It also implements the {@link Interceptable} interface which means that it contains a list of {@link Interceptor interceptors},
 * on which IoC and lifecycle is propagated as well.
 * <p>
 * In the case of the {@link #connectionProvider} being present, then it also binds the {@link #value} to the
 * {@link ConnectionProvider} by the means of {@link ConnectionManager#bind(Object, ConnectionProvider)} when the
 * {@link #initialise()} phase is executed. That bound will be broken on the {@link #stop()} phase by using
 * {@link ConnectionManager#unbind(Object)}
 *
 * @since 4.0
 */
public final class LifecycleAwareConfigurationInstance<T> extends AbstractInterceptable implements ConfigurationInstance<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAwareConfigurationInstance.class);

  private final String name;
  private final RuntimeConfigurationModel model;
  private final T value;
  private final Optional<ConnectionProvider> connectionProvider;

  private ConfigurationStats configurationStats;

  @Inject
  private TimeSupplier timeSupplier;

  @Inject
  private MuleContext muleContext;

  @Inject
  private ConnectionManager connectionManager;

  /**
   * Creates a new instance
   *
   * @param name this configuration's name
   * @param model the {@link ConfigurationModel} for this instance
   * @param value the actual configuration instance
   * @param interceptors the {@link List} of {@link Interceptor interceptors} that applies
   * @param connectionProvider an {@link Optional} containing the {@link ConnectionProvider} to use
   */
  public LifecycleAwareConfigurationInstance(String name, RuntimeConfigurationModel model, T value,
                                             List<Interceptor> interceptors, Optional<ConnectionProvider> connectionProvider) {
    super(interceptors);
    this.name = name;
    this.model = model;
    this.value = value;
    this.connectionProvider = connectionProvider;
  }

  /**
   * Initialises this instance by
   * <ul>
   * <li>Initialising the {@link #configurationStats}</li>
   * <li>Performs dependency injection on the {@link #value} and each item in {@link #getInterceptors()}</li>
   * <li>Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}</li>
   * </ul>
   *
   * @throws InitialisationException if an exception is found
   */
  @Override
  public void initialise() throws InitialisationException {
    try {
      initStats();
      doInitialise();
    } catch (Exception e) {
      if (e instanceof InitialisationException) {
        throw (InitialisationException) e;
      } else {
        throw new InitialisationException(e, this);
      }
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public void start() throws MuleException {
    if (connectionProvider.isPresent()) {
      startIfNeeded(connectionProvider);
      if (!connectionManager.hasBinding(value)) {
        connectionManager.bind(value, connectionProvider.get());
      }
    }
    startIfNeeded(value);
    super.start();
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}. Also triggers a
   * {@link ConfigurationInstanceNotification} that is being stopped.
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public void stop() throws MuleException {
    try {
      stopIfNeeded(value);
      if (connectionProvider.isPresent()) {
        connectionManager.unbind(value);
        stopIfNeeded(connectionProvider);
      }
      super.stop();
    } finally {
      muleContext.fireNotification(new ConfigurationInstanceNotification(this, CONFIGURATION_STOPPED));
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
   */
  @Override
  public void dispose() {
    disposeIfNeeded(value, LOGGER);
    disposeIfNeeded(connectionProvider, LOGGER);
    super.dispose();
  }

  private void doInitialise() throws InitialisationException {
    if (connectionProvider.isPresent()) {
      initialiseIfNeeded(connectionProvider, true, muleContext);
      connectionManager.bind(value, connectionProvider.get());
    }

    initialiseIfNeeded(value, true, muleContext);
    super.initialise();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConnectionProvider> getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RuntimeConfigurationModel getModel() {
    return model;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue() {
    return value;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if invoked before {@link #initialise()}
   */
  @Override
  public ConfigurationStats getStatistics() {
    checkState(configurationStats != null, "can't get statistics before initialise() is invoked");
    return configurationStats;
  }

  private void initStats() {
    if (timeSupplier == null) {
      timeSupplier = new TimeSupplier();
    }

    configurationStats = new DefaultMutableConfigurationStats(timeSupplier);
  }
}
