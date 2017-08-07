/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.config.ConfigurationInstanceNotification.CONFIGURATION_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationInstanceNotification;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.time.TimeSupplier;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.internal.loader.AbstractInterceptable;
import org.mule.runtime.module.extension.internal.runtime.connectivity.NoConnectivityTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;

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
public final class LifecycleAwareConfigurationInstance extends AbstractInterceptable implements ConfigurationInstance {

  private static final Logger LOGGER = getLogger(LifecycleAwareConfigurationInstance.class);
  private static final String DO_TEST_CONNECTIVITY_PROPERTY_NAME = "doTestConnectivity";

  private final String name;
  private final ConfigurationModel model;
  private final Object value;
  private final Function<Event, ConfigurationState> configurationStateResolver;
  private final Optional<ConnectionProvider> connectionProvider;

  private ConfigurationStats configurationStats;

  @Inject
  private TimeSupplier timeSupplier;

  @Inject
  private MuleContext muleContext;

  @Inject
  private LockFactory lockFactory;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private ConnectionManagerAdapter connectionManager;

  private Lock testConnectivityLock;
  private Scheduler retryScheduler;

  private volatile boolean initialized = false;
  private volatile boolean started = false;

  private boolean doTestConnectivity = getDoTestConnectivityProperty();

  /**
   * Creates a new instance
   *
   * @param name this configuration's name
   * @param model the {@link ConfigurationModel} for this instance
   * @param value the actual configuration instance
   * @param interceptors the {@link List} of {@link Interceptor interceptors} that applies
   * @param connectionProvider an {@link Optional} containing the {@link ConnectionProvider} to use
   */
  public LifecycleAwareConfigurationInstance(String name,
                                             ConfigurationModel model,
                                             Object value,
                                             Function<Event, ConfigurationState> configurationStateResolver,
                                             List<Interceptor> interceptors,
                                             Optional<ConnectionProvider> connectionProvider) {
    super(interceptors);
    this.name = name;
    this.model = model;
    this.value = value;
    this.configurationStateResolver = configurationStateResolver;
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
  public synchronized void initialise() throws InitialisationException {
    if (!initialized) {
      initialized = true;
      try {
        initStats();
        doInitialise();
        super.initialise();
      } catch (Exception e) {
        if (e instanceof InitialisationException) {
          throw (InitialisationException) e;
        } else {
          throw new InitialisationException(e, this);
        }
      }
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public synchronized void start() throws MuleException {
    if (!started) {
      started = true;
      testConnectivityLock = lockFactory.createLock(this.getClass().getName() + "-testConnectivity-" + getName());
      if (connectionProvider.isPresent()) {
        startIfNeeded(connectionProvider);
        if (!connectionManager.hasBinding(value)) {
          connectionManager.bind(value, connectionProvider.get());
        }
        if (doTestConnectivity) {
          testConnectivity();
        }
      }
      startIfNeeded(value);
      super.start();
    }
  }

  private void testConnectivity() throws MuleException {
    ConnectionProvider provider = connectionProvider.get();
    if (provider instanceof NoConnectivityTest) {
      return;
    }

    RetryPolicyTemplate retryTemplate = connectionManager.getRetryTemplateFor(provider);

    RetryCallback retryCallback = new RetryCallback() {

      @Override
      public void doWork(RetryContext context) throws Exception {
        final boolean lockAcquired = testConnectivityLock.tryLock();
        if (lockAcquired) {
          LOGGER.info("Doing testConnectivity() for config " + getName());
          try {
            ConnectionValidationResult result = connectionManager.testConnectivity(LifecycleAwareConfigurationInstance.this);
            if (result.isValid()) {
              context.setOk();
            } else {
              context.setFailed(result.getException());
              throw new ConnectionException(format("Connectivity test failed for config '%s'", getName()), result.getException());
            }
          } finally {
            testConnectivityLock.unlock();
          }
        } else {
          LOGGER.warn("There is a testConnectivity() already running for config " + getName());
        }
      }

      @Override
      public String getWorkDescription() {
        return format("Testing connectivity for config '%s'", getName());
      }

      @Override
      public Object getWorkOwner() {
        return value;
      }
    };

    try {
      retryTemplate.execute(retryCallback, retryScheduler);
    } catch (Exception e) {
      throw new DefaultMuleException(createStaticMessage(format("Could not perform connectivity testing for config '%s'",
                                                                getName())),
                                     e);
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}. Also triggers a
   * {@link ConfigurationInstanceNotification} that is being stopped.
   *
   * @throws MuleException if an exception is found
   */
  @Override
  public synchronized void stop() throws MuleException {
    if (started) {
      started = false;
      try {
        stopIfNeeded(value);
        if (connectionProvider.isPresent()) {
          testConnectivityLock.lock();
          try {
            connectionManager.unbind(value);
            stopIfNeeded(connectionProvider);
          } finally {
            testConnectivityLock.unlock();
          }
        }
        super.stop();
      } finally {
        muleContext.fireNotification(new ConfigurationInstanceNotification(this, CONFIGURATION_STOPPED));
      }
    }
  }

  /**
   * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
   */
  @Override
  public synchronized void dispose() {
    if (initialized) {
      initialized = false;
      if (retryScheduler != null) {
        retryScheduler.stop();
      }
      disposeIfNeeded(value, LOGGER);
      disposeIfNeeded(connectionProvider, LOGGER);
      configurationStats = null;
      testConnectivityLock = null;
      super.dispose();
    }
  }

  private void doInitialise() throws InitialisationException {
    if (connectionProvider.isPresent()) {
      initialiseIfNeeded(connectionProvider, true, muleContext);
      connectionManager.bind(value, connectionProvider.get());
    }

    initialiseIfNeeded(value, true, muleContext);
    retryScheduler = schedulerService.ioScheduler();
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
  public ConfigurationState getState(Object event) {
    return configurationStateResolver.apply((Event) event);
  }

  private void initStats() {
    if (timeSupplier == null) {
      timeSupplier = new TimeSupplier();
    }

    configurationStats = new DefaultMutableConfigurationStats(timeSupplier);
  }

  private boolean getDoTestConnectivityProperty() {
    return System.getProperty(DO_TEST_CONNECTIVITY_PROPERTY_NAME) != null
        ? valueOf(System.getProperty(DO_TEST_CONNECTIVITY_PROPERTY_NAME))
        : true;
  }
}
