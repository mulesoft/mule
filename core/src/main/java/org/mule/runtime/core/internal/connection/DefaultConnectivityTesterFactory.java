/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.ASYNC_TEST_CONNECTIVITY_TIMEOUT_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import static java.lang.Boolean.valueOf;
import static java.lang.Integer.getInteger;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.concurrent.locks.Lock;

import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * Implementation of {@link ConnectivityTesterFactory} that takes into account the reconnection strategy.
 *
 * @since 4.4
 */
public class DefaultConnectivityTesterFactory implements ConnectivityTesterFactory {

  public static final String DO_TEST_CONNECTIVITY_PROPERTY_NAME = "doTestConnectivity";
  private static final String ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "async.test.connectivity.lock.timeout";

  private static final Integer DEFAULT_ASYNC_TEST_CONNECTIVITY_TIMEOUT = 30000;
  private static final Integer DEFAULT_ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT = 10000;

  private static final int ASYNC_TEST_CONNECTIVITY_TIMEOUT =
      getInteger(ASYNC_TEST_CONNECTIVITY_TIMEOUT_PROPERTY, DEFAULT_ASYNC_TEST_CONNECTIVITY_TIMEOUT);

  private static final int ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT =
      getInteger(ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT_PROPERTY, DEFAULT_ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT);

  private static final Logger LOGGER = getLogger(DefaultConnectivityTesterFactory.class);

  private ConnectionManagerAdapter connectionManager;

  private LockFactory lockFactory;

  private SchedulerService schedulerService;

  private final LazyValue<Boolean> doTestConnectivity = new LazyValue<>(this::getDoTestConnectivityProperty);

  @Inject
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Inject
  public void setLockFactory(LockFactory lockFactory) {
    this.lockFactory = lockFactory;
  }

  @Inject
  public void setConnectionManager(ConnectionManagerAdapter connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public ConnectivityTester create(String name) {
    final Lock testConnectivityLock = lockFactory.createLock(this.getClass().getName() + "-testConnectivity-" + name);
    return new ConnectivityTester() {

      @Override
      public void testConnectivity(ConnectionProvider provider, ConfigurationInstance configurationInstance)
          throws MuleException {
        if (provider instanceof NoConnectivityTest || !doTestConnectivity.get()) {
          return;
        }

        Scheduler retryScheduler = schedulerService.ioScheduler();
        RetryPolicyTemplate retryTemplate = (RetryPolicyTemplate) connectionManager.getRetryTemplateFor(provider);
        ReconnectionConfig reconnectionConfig = connectionManager.getReconnectionConfigFor(provider);
        final Latch latch = new Latch();
        RetryCallback retryCallback = new RetryCallback() {

          @Override
          public void doWork(RetryContext context) throws Exception {
            try {
              if (testConnectivityLock != null) {
                final boolean lockAcquired = testConnectivityLock.tryLock(ASYNC_TEST_CONNECTIVITY_LOCK_TIMEOUT, MILLISECONDS);
                if (lockAcquired) {
                  LOGGER.debug("Doing testConnectivity() for config '{}'", name);
                  try {
                    ConnectionValidationResult result = connectionManager.testConnectivity(configurationInstance);
                    if (result.isValid()) {
                      context.setOk();
                    } else {
                      handleTestConnectivityFailure(name, reconnectionConfig, context, result);
                    }
                  } finally {
                    testConnectivityLock.unlock();
                  }
                } else {
                  LOGGER.warn("There is a testConnectivity() already running for config '{}'", name);
                }
              }
            } finally {
              latch.countDown();
            }
          }

          @Override
          public String getWorkDescription() {
            return format("Testing connectivity for config '%s'", name);
          }

          @Override
          public Object getWorkOwner() {
            return configurationInstance.getValue();
          }
        };

        try {
          retryTemplate.execute(retryCallback, retryScheduler);
        } catch (Exception e) {
          throw new DefaultMuleException(createStaticMessage(format("Could not perform connectivity testing for config '%s'",
                                                                    name)),
                                         e);
        } finally {
          if (retryTemplate.isAsync()) {
            try {
              latch.await(ASYNC_TEST_CONNECTIVITY_TIMEOUT, MILLISECONDS);
            } catch (InterruptedException e) {
              LOGGER.warn("InterruptedException while waiting for the test connectivity to finish", e);
            }
          }
          if (retryScheduler != null) {
            retryScheduler.stop();
          }
        }
      }

      /**
       * Runs the provided task as soon as any pending connectivity testing is finished, or immediately if there is not any.
       *
       * @param task
       */
      @Override
      public void withTestConnectivityLock(CheckedRunnable task) {
        testConnectivityLock.lock();
        try {
          task.run();
        } finally {
          testConnectivityLock.unlock();
        }
      }
    };
  }

  private void handleTestConnectivityFailure(String name, ReconnectionConfig reconnectionConfig, RetryContext context,
                                             ConnectionValidationResult result)
      throws ConnectionException {
    if ((reconnectionConfig.isFailsDeployment())) {
      context.setFailed(result.getException());
      throw new ConnectionException(format("Connectivity test failed for config '%s'", name), result.getException());
    } else {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(format("Connectivity test failed for config '%s'. Application deployment will continue. Error was: %s",
                           name, result.getMessage()),
                    result.getException());
      }
    }
  }

  private boolean getDoTestConnectivityProperty() {
    return getProperty(DO_TEST_CONNECTIVITY_PROPERTY_NAME) != null
        ? valueOf(getProperty(DO_TEST_CONNECTIVITY_PROPERTY_NAME))
        : true;
  }
}
