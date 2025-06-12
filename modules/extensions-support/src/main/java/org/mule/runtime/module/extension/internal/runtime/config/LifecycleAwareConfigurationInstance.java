/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.config.ConfigurationInstanceNotification.CONFIGURATION_STOPPED;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.config.ConfigurationInstanceNotification;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectivityTester;
import org.mule.runtime.core.internal.connection.ConnectivityTesterFactory;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * Implementation of {@link ConfigurationInstance} which propagates dependency injection and lifecycle phases into the contained
 * configuration {@link #value} and {@link #connectionProvider} (if present).
 * <p>
 * In the case of the {@link #connectionProvider} being present, then it also binds the {@link #value} to the
 * {@link ConnectionProvider} by the means of {@link ConnectionManager#bind(Object, ConnectionProvider)} when the
 * {@link #initialise()} phase is executed. That bound will be broken on the {@link #stop()} phase by using
 * {@link ConnectionManager#unbind(Object)}
 *
 * @since 4.0
 */
public final class LifecycleAwareConfigurationInstance implements ConfigurationInstance, Lifecycle {

  private static final Logger LOGGER = getLogger(LifecycleAwareConfigurationInstance.class);

  private final String name;
  private final ConfigurationModel model;
  private final Object value;
  private final ConfigurationState configurationState;
  private final Optional<ConnectionProvider> connectionProvider;

  private ConfigurationStats configurationStats;

  private TimeSupplier timeSupplier;
  private NotificationDispatcher notificationFirer;
  private ConnectionManagerAdapter connectionManager;
  private ConnectivityTesterFactory connectivityTesterFactory;
  private Injector injector;

  private volatile boolean initialized = false;
  private volatile boolean started = false;

  private ConnectivityTester connectivityTester;

  /**
   * Creates a new instance
   *
   * @param name               this configuration's name
   * @param model              the {@link ConfigurationModel} for this instance
   * @param value              the actual configuration instance
   * @param connectionProvider an {@link Optional} containing the {@link ConnectionProvider} to use
   */
  public LifecycleAwareConfigurationInstance(String name,
                                             ConfigurationModel model,
                                             Object value,
                                             ConfigurationState configurationState,
                                             Optional<ConnectionProvider> connectionProvider) {
    this.name = name;
    this.model = model;
    this.value = value;
    this.configurationState = configurationState;
    this.connectionProvider = connectionProvider;
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    LOGGER.debug("Initializing LifecycleAwareConfigurationInstance '{}'", getName());

    if (!initialized) {
      initialized = true;
      try {
        initStats();
        doInitialise();
      } catch (Exception e) {
        LOGGER.error(format("Error initializing LifecycleAwareConfigurationInstance '%s'", getName()), e);

        if (e instanceof InitialisationException) {
          throw (InitialisationException) e;
        } else {
          throw new InitialisationException(e, this);
        }
      }
    }
  }

  @Override
  public synchronized void start() throws MuleException {
    LOGGER.debug("Starting LifecycleAwareConfigurationInstance '{}'", getName());

    if (!started) {
      started = true;
      if (connectionProvider.isPresent()) {
        startIfNeeded(connectionProvider);
        if (!connectionManager.hasBinding(value)) {
          connectionManager.bind(value, connectionProvider.get());
        }

        connectivityTester = connectivityTesterFactory.create(getName());
        connectivityTester.testConnectivity(connectionProvider.get(), this);
      }
      startIfNeeded(value);
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value}. Also triggers a {@link ConfigurationInstanceNotification} that
   * is being stopped.
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public synchronized void stop() throws MuleException {
    LOGGER.debug("Stopping LifecycleAwareConfigurationInstance '{}'", getName());

    if (started) {
      started = false;
      try {
        stopIfNeeded(value);
        if (connectionProvider.isPresent()) {
          if (connectivityTester == null) {
            unbindConnection();
          } else {
            connectivityTester.withTestConnectivityLock(this::unbindConnection);
          }
        }
        connectivityTester = null;
      } finally {
        notificationFirer.dispatch(new ConfigurationInstanceNotification(this, CONFIGURATION_STOPPED));
      }
    }
  }

  protected void unbindConnection() throws MuleException {
    connectionManager.unbind(value);
    stopIfNeeded(connectionProvider);
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value}.
   */
  @Override
  public synchronized void dispose() {
    LOGGER.debug("Disposing LifecycleAwareConfigurationInstance '{}'", getName());

    if (initialized) {
      initialized = false;
      disposeIfNeeded(value, LOGGER);
      disposeIfNeeded(connectionProvider, LOGGER);
      configurationStats = null;
    }
  }

  private void doInitialise() throws InitialisationException {
    if (connectionProvider.isPresent()) {
      initialiseIfNeeded(connectionProvider, injector);
      initialiseIfNeeded(connectionManager);
      connectionManager.bind(value, connectionProvider.get());
    }

    initialiseIfNeeded(value, injector);
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
  public ConfigurationModel getModel() {
    return model;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getValue() {
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

  @Override
  public ConfigurationState getState() {
    return configurationState;
  }

  private void initStats() {
    if (timeSupplier == null) {
      timeSupplier = new LocalTimeSupplier();
    }

    configurationStats = new DefaultMutableConfigurationStats(timeSupplier);
  }

  @Inject
  public void setTimeSupplier(TimeSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

  @Inject
  public void setNotificationFirer(NotificationDispatcher notificationFirer) {
    this.notificationFirer = notificationFirer;
  }

  @Inject
  public void setConnectionManager(ConnectionManagerAdapter connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Inject
  public void setConnectivityTesterFactory(ConnectivityTesterFactory connectivityTesterFactory) {
    this.connectivityTesterFactory = connectivityTesterFactory;
  }

  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    setInjector(muleContext.getInjector());
  }
}
