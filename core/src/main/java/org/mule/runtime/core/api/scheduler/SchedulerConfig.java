/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Provides a fluent way of customizing a {@link Scheduler} obtained through the {@link SchedulerService}.
 *
 * @since 4.0
 */
public class SchedulerConfig {

  /**
   * @return a default configuration, which can be further customized.
   */
  public static SchedulerConfig config() {
    return new SchedulerConfig();
  }

  private final Integer maxConcurrentTasks;
  private final String schedulerPrefix;
  private final String schedulerName;
  private final Boolean waitAllowed;
  private final Supplier<Long> shutdownTimeoutMillis;

  private SchedulerConfig() {
    this.maxConcurrentTasks = null;
    this.schedulerPrefix = null;
    this.schedulerName = null;
    this.waitAllowed = null;
    this.shutdownTimeoutMillis = () -> null;
  }

  private SchedulerConfig(Integer maxConcurrentTasks, String schedulerPrefix, String schedulerName,
                          Boolean waitAllowed, Supplier<Long> shutdownTimeoutMillis) {
    this.maxConcurrentTasks = maxConcurrentTasks;
    this.schedulerPrefix = schedulerPrefix;
    this.schedulerName = schedulerName;
    this.waitAllowed = waitAllowed;
    this.shutdownTimeoutMillis = shutdownTimeoutMillis;
  }

  /**
   * Sets the max tasks that can be run at the same time for the target {@link Scheduler}.
   * <p>
   * This is useful to apply throttling on the target {@link Scheduler}. The way exceeding tasks will be handled is determined by
   * the target {@link Scheduler}.
   * 
   * @param maxConcurrentTasks how many tasks can be running at the same time for the target {@link Scheduler}.
   * @return the updated configuration.
   */
  public SchedulerConfig withMaxConcurrentTasks(int maxConcurrentTasks) {
    return new SchedulerConfig(maxConcurrentTasks, schedulerPrefix, schedulerName, waitAllowed, shutdownTimeoutMillis);
  }

  /**
   * @return how many tasks can be running at the same time for the target {@link Scheduler}.
   */
  public Integer getMaxConcurrentTasks() {
    return maxConcurrentTasks;
  }

  /**
   * Sets the prefix to prepend to the name for the target {@link Scheduler}, which will override the default one.
   * 
   * @param schedulerPrefix the prefix for the name for the target {@link Scheduler}.
   * @return the updated configuration.
   */
  public SchedulerConfig withPrefix(String schedulerPrefix) {
    return new SchedulerConfig(maxConcurrentTasks, schedulerPrefix, schedulerName, waitAllowed, shutdownTimeoutMillis);
  }

  /**
   * Sets the name for the target {@link Scheduler}, which will override the default one.
   * 
   * @param schedulerName the name for the target {@link Scheduler}.
   * @return the updated configuration.
   */
  public SchedulerConfig withName(String schedulerName) {
    return new SchedulerConfig(maxConcurrentTasks, schedulerPrefix, schedulerName, waitAllowed, shutdownTimeoutMillis);
  }

  /**
   * @return the name for the target {@link Scheduler}.
   */
  public String getSchedulerName() {
    return schedulerPrefix == null ? schedulerName : format("[%s].%s", schedulerPrefix, schedulerName);
  }

  /**
   * @return {@code true} if {@link #withName(String)} was called with a non-null value, {@code false} otherwise.
   */
  public boolean hasName() {
    return schedulerName != null;
  }

  /**
   * whether the threads of the target custom {@link Scheduler} may block to wait when dispatching to a busy {@link Scheduler}.
   * <p>
   * This is only applicable for <b>custom</b> {@link Scheduler}s. This behaviour cannot be changed for the runtime managed
   * {@link Scheduler}.
   * 
   * @return the updated configuration
   */
  public SchedulerConfig withWaitAllowed(boolean waitAllowed) {
    return new SchedulerConfig(maxConcurrentTasks, schedulerPrefix, schedulerName, waitAllowed, shutdownTimeoutMillis);
  }

  /**
   * @return whether the threads of the target custom {@link Scheduler} may block to wait when dispatching to a busy
   *         {@link Scheduler}.
   */
  public Boolean getWaitAllowed() {
    return waitAllowed;
  }

  /**
   * Sets the graceful shutdown timeout to use when stopping the target {@link Scheduler}.
   * 
   * @param shutdownTimeoutSupplier a supplier of the value of the timeout to use when gracefully stopping the target
   *        {@link Scheduler}, expressed in the provided {@link TimeUnit}.
   * @param shutdownTimeoutUnit the unit of the timeout to use when gracefully stopping the target {@link Scheduler}.
   * @return the updated configuration
   */
  public SchedulerConfig withShutdownTimeout(Supplier<Long> shutdownTimeoutSupplier, TimeUnit shutdownTimeoutUnit) {
    requireNonNull(shutdownTimeoutUnit);

    return new SchedulerConfig(maxConcurrentTasks, schedulerPrefix, schedulerName, waitAllowed, () -> {
      long shutdownTimeout = shutdownTimeoutSupplier.get();
      validateTimeoutValue(shutdownTimeout);
      return shutdownTimeoutUnit.toMillis(shutdownTimeout);
    });
  }

  /**
   * Sets the graceful shutdown timeout to use when stopping the target {@link Scheduler}.
   * 
   * @param shutdownTimeout the value of the timeout to use when gracefully stopping the target {@link Scheduler}, expressed in
   *        the provided {@link TimeUnit}.
   * @param shutdownTimeoutUnit the unit of the timeout to use when gracefully stopping the target {@link Scheduler}.
   * @return the updated configuration
   */
  public SchedulerConfig withShutdownTimeout(long shutdownTimeout, TimeUnit shutdownTimeoutUnit) {
    requireNonNull(shutdownTimeoutUnit);
    validateTimeoutValue(shutdownTimeout);
    return withShutdownTimeout(() -> shutdownTimeout, shutdownTimeoutUnit);
  }

  private void validateTimeoutValue(long shutdownTimeout) {
    if (shutdownTimeout < 0) {
      throw new IllegalArgumentException(format("'shutdownTimeout' must be a possitive long. %d passed", shutdownTimeout));
    }
  }

  /**
   * @return a supplier of the timeout to use when gracefully stopping the target {@link Scheduler}, in millis.
   */
  public Supplier<Long> getShutdownTimeoutMillis() {
    return shutdownTimeoutMillis;
  }
}
