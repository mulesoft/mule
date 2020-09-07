/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import com.google.common.collect.Multimap;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ConfigurationExpirationMonitor} which schedules tasks that run on a given {@link #frequency}
 * to check for dynamic configuration instances which should be expired. The selected instances are expired by being unregistered
 * and having the shutdown lifecycle applied to them.
 * <p/>
 * Instances of this class are immutable and should be built through a {@link Builder} which can be obtained through the
 * {@link Builder#newBuilder(ExtensionRegistry, MuleContext)} method
 *
 * @since 4.0
 */
public final class DefaultConfigurationExpirationMonitor implements ConfigurationExpirationMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigurationExpirationMonitor.class);

  /**
   * A builder object for instances of {@link DefaultConfigurationExpirationMonitor}
   */
  public static class Builder {

    /**
     * Creates a new builder instance
     *
     * @param extensionRegistry the {@link ExtensionRegistry} instance used to obtain the configuration instances
     * @param muleContext the {@link MuleContext} which owns the configuration instances
     * @return a new {@link Builder}
     */
    public static Builder newBuilder(ExtensionRegistry extensionRegistry, MuleContext muleContext) {
      Builder builder = new Builder();
      builder.manager.extensionRegistry = extensionRegistry;
      builder.manager.muleContext = muleContext;

      return builder;
    }

    private DefaultConfigurationExpirationMonitor manager = new DefaultConfigurationExpirationMonitor();

    private Builder() {}

    /**
     * Specifies how often should this instance check for expired config instances
     *
     * @param frequency a scalar time value
     * @param timeUnit a {@link TimeUnit} which qualifies the {@code frequency}
     * @return {@code this} instance
     */
    public Builder runEvery(long frequency, TimeUnit timeUnit) {
      manager.frequency = frequency;
      manager.timeUnit = timeUnit;

      return this;
    }

    /**
     * Provides a {@link BiConsumer} which receives each expired config instance and provides the behavior of how to expire it.
     * The {@link BiConsumer} will receive the instance registration key as the first argument and the instance itself as the
     * second
     *
     * @param expirationHandler a {@link BiConsumer} which acts as a expiration handler
     * @return {@code this} instance
     */
    public Builder onExpired(BiConsumer<String, ConfigurationInstance> expirationHandler) {
      manager.expirationHandler = expirationHandler;
      return this;
    }

    /**
     * Validates the provided configuration and returns an actual {@link ConfigurationExpirationMonitor}.
     *
     * @return a {@link ConfigurationExpirationMonitor}
     */
    public ConfigurationExpirationMonitor build() {
      checkArgument(manager.extensionRegistry != null, "extensionRegistry cannot be null");
      checkArgument(manager.muleContext != null, "muleContext cannot be null");
      checkArgument(manager.frequency > 0, "frequency must be greater than zero");
      checkArgument(manager.timeUnit != null, "timeUnit cannot be null");

      return manager;
    }

  }

  private ExtensionRegistry extensionRegistry;
  private MuleContext muleContext;
  private long frequency;
  private TimeUnit timeUnit;
  private BiConsumer<String, ConfigurationInstance> expirationHandler;

  private Scheduler executor;
  private ScheduledFuture<?> scheduledMonitoring;

  private DefaultConfigurationExpirationMonitor() {}

  /**
   * Starts a scheduler which fires expiration tasks on the given {@link #frequency} and executes the {@link #expirationHandler}
   * on each matching configuration instance
   *
   */
  @Override
  public void beginMonitoring() {
    // TODO: Change the executor type when MULE-8870 is implemented
    executor = muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig()
        .withName("extension.expiration.manager").withShutdownTimeout(30, SECONDS));
    scheduledMonitoring = executor.scheduleWithFixedDelay(() -> expire(), frequency, frequency, timeUnit);
  }

  private void expire() {
    if (stopChecking()) {
      return;
    }

    LOGGER.debug("Running configuration expiration cycle");
    try {
      Multimap<String, ConfigurationInstance> expired = extensionRegistry.getExpiredConfigs();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(expired.isEmpty() ? "No expired configuration instances were found" : "Found {} expired configurations",
                     expired.size());
      }

      expired.entries().stream().forEach(entry -> handleExpiration(entry.getKey(), entry.getValue()));
    } catch (Exception e) {
      LOGGER.error("Found exception trying to expire idle configurations. Will try again on next cycle", e);
    }

  }

  private void handleExpiration(String key, ConfigurationInstance config) {
    if (stopChecking()) {
      return;
    }

    try {
      expirationHandler.accept(key, config);
      LOGGER.debug("Configuration of key {} was expired", key);
    } catch (Exception e) {
      LOGGER
          .error(String.format("Could not process expiration for dynamic config '%s' of type '%s'. Will try again on next cycle",
                               key, config.getClass().getName()),
                 e);
    }
  }

  /**
   * Shutdowns the scheduler that executes the expiration tasks. It waits up to 30 seconds for it to shutdown and it throws a
   * {@link MuleException} if it could not be stopped
   */
  @Override
  public void stopMonitoring() {
    scheduledMonitoring.cancel(false);
    executor.stop();
  }

  private boolean stopChecking() {
    return muleContext.isStopping() || muleContext.isStopped();
  }
}
