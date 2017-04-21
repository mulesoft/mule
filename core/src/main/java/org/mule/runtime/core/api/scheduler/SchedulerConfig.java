/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.RejectionAction.DEFAULT;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

/**
 * Provides a fluent way of customizing a {@link Scheduler} obtained through the {@link SchedulerService}.
 *
 * @since 4.0
 */
public class SchedulerConfig {

  /**
   * Different possible actions to handle the scenario where a task is dispatched to a busy {@link Scheduler}.
   * <p>
   * A {@link Scheduler} is considered busy when all of its threads are busy and it cannot accept a new task for execution.
   */
  public enum RejectionAction {
    /**
     * The actual {@link RejectedExecutionHandler} of the target {@link Scheduler} will depend on the type of scheduler the thread
     * is from. For cpu-bound threads (cpuLight and cpuIntensive) it will be <b>abort</b>, and for the other cases it will be
     * <b>wait</b>.
     */
    DEFAULT,

    /**
     * The {@link RejectedExecutionHandler} of the target {@link Scheduler} will cause the dispatcher thread to wait for the task
     * to be taken by the target scheduler, effectively blocking until that happens.
     */
    WAIT;
  }

  /**
   * @return a default configuration, which can be further customized.
   */
  public static SchedulerConfig config() {
    return new SchedulerConfig();
  }

  private final Integer maxConcurrentTasks;
  private final String schedulerName;
  private final RejectionAction rejectionAction;
  private final Long shutdownTimeoutMillis;

  private SchedulerConfig() {
    this.maxConcurrentTasks = null;
    this.schedulerName = null;
    this.rejectionAction = DEFAULT;
    this.shutdownTimeoutMillis = null;
  }

  private SchedulerConfig(Integer maxConcurrentTasks, String schedulerName, RejectionAction rejectionAction,
                          Long shutdownTimeoutMillis) {
    this.maxConcurrentTasks = maxConcurrentTasks;
    this.schedulerName = schedulerName;
    this.rejectionAction = rejectionAction;
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
    return new SchedulerConfig(maxConcurrentTasks, schedulerName, rejectionAction, shutdownTimeoutMillis);
  }

  /**
   * @return how many tasks can be running at the same time for the target {@link Scheduler}.
   */
  public Integer getMaxConcurrentTasks() {
    return maxConcurrentTasks;
  }

  /**
   * Sets the name for the target {@link Scheduler}, which will override the default one.
   * 
   * @param schedulerName the name for the target {@link Scheduler}.
   * @return the updated configuration.
   */
  public SchedulerConfig withName(String schedulerName) {
    return new SchedulerConfig(maxConcurrentTasks, schedulerName, rejectionAction, shutdownTimeoutMillis);
  }

  /**
   * @return the name for the target {@link Scheduler}.
   */
  public String getSchedulerName() {
    return schedulerName;
  }

  /**
   * Sets the rejection policy to use when dispatching to a busy {@link Scheduler}.
   * <p>
   * This is only applicable for <b>custom</b> {@link Scheduler}s. The policy cannot be changed for the runtime managed
   * {@link Scheduler}.
   * 
   * @see SchedulerBusyException
   * 
   * @return the updated configuration
   */
  public SchedulerConfig withRejectionAction(RejectionAction rejectionAction) {
    requireNonNull(rejectionAction);
    return new SchedulerConfig(maxConcurrentTasks, schedulerName, rejectionAction, shutdownTimeoutMillis);
  }

  /**
   * @return the {@link RejectionAction} for the target custom {@link Scheduler}.
   */
  public RejectionAction getRejectionAction() {
    return rejectionAction;
  }

  /**
   * Sets the graceful shutdown timeout to use when stopping the target {@link Scheduler}.
   * 
   * @param shutdownTimeout the value of the timeout to use when gracefully stopping the target {@link Scheduler}.
   * @param shutdownTimeoutUnit the unit of the timeout to use when gracefully stopping the target {@link Scheduler}.
   * @return the updated configuration
   */
  public SchedulerConfig withShutdownTimeout(long shutdownTimeout, TimeUnit shutdownTimeoutUnit) {
    if (shutdownTimeout < 0) {
      throw new IllegalArgumentException(format("'shutdownTimeout' must be a possitive long. %d passed", shutdownTimeout));
    }
    requireNonNull(shutdownTimeoutUnit);

    return new SchedulerConfig(maxConcurrentTasks, schedulerName, rejectionAction, shutdownTimeoutUnit.toMillis(shutdownTimeout));
  }

  /**
   * @return the timeout to use when gracefully stopping the target {@link Scheduler}, in millis.
   */
  public Long getShutdownTimeoutMillis() {
    return shutdownTimeoutMillis;
  }
}
