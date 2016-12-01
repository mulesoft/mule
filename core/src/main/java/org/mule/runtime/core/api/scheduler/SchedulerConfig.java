/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;

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

  private Integer maxConcurrentTasks;
  private String schedulerName;

  /**
   * Sets the max tasks that can be run at the same time for the target {@link Scheduler}.
   * <p>
   * This is useful to apply throttling on the target {@link Scheduler}. Exceeding tasks will block the caller, until a running
   * task is finished.
   * 
   * @param maxConcurrentTasks how many tasks can be running at the same time for the target {@link Scheduler}.
   * @return the updated configuration.
   */
  public SchedulerConfig withMaxConcurrentTasks(int maxConcurrentTasks) {
    this.maxConcurrentTasks = maxConcurrentTasks;
    return this;
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
    this.schedulerName = schedulerName;
    return this;
  }

  /**
   * @return the name for the target {@link Scheduler}.
   */
  public String getSchedulerName() {
    return schedulerName;
  }
}
