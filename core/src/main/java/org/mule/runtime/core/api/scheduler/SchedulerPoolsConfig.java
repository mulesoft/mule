/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import java.util.concurrent.Executor;

/**
 * Parameters to use when building the {@link Executor}s for the scheduler service.
 * 
 * @since 4.0
 */
public interface SchedulerPoolsConfig {

  /**
   * @return the maximum time (in milliseconds) to wait until all tasks in all the runtime thread pools have completed execution
   *         when stopping the scheduler service.
   */
  Long getGracefulShutdownTimeout();

  /**
   * @return the number of threads to keep in the {@code cpu_lite} pool, even if they are idle.
   */
  Integer getCpuLightPoolSize();

  /**
   * @return the size of the queue to use for holding {@code cpu_lite} tasks before they are executed.
   */
  Integer getCpuLightQueueSize();

  /**
   * @return the number of threads to keep in the {@code I/O} pool.
   */
  Integer getIoCorePoolSize();

  /**
   * @return the maximum number of threads to allow in the {@code I/O} pool.
   */
  Integer getIoMaxPoolSize();

  /**
   * @return the size of the queue to use for holding {@code I/O} tasks before they are executed.
   */
  Integer getIoQueueSize();

  /**
   * @return when the number of threads in the {@code I/O} pool is greater than {@link #getIoCorePoolSize()}, this is the maximum
   *         time (in milliseconds) that excess idle threads will wait for new tasks before terminating.
   */
  Long getIoKeepAlive();

  /**
   * @return the number of threads to keep in the {@code cpu_intensive} pool, even if they are idle.
   */
  Integer getCpuIntensivePoolSize();

  /**
   * @return the size of the queue to use for holding {@code cpu_intensive} tasks before they are executed.
   */
  Integer getCpuIntensiveQueueSize();

  /**
   * @return the prefix to prepend to the names of the threads of the pools created for a scheduler with this configuration.
   */
  String getThreadNamePrefix();
}
