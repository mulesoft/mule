/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import org.mule.runtime.api.service.Service;

/**
 * Provides access to the different schedulers and thread pools that exist in the Mule runtime, allowing an artifact to schedule
 * tasks on those.
 * <p>
 * This interface provides access to different schedulers, each with its own tuning options, optimized for some specific kind of
 * work. Artifacts that need to dispatch tasks need to carefully determine what's the scheduler that best fits the nature of the
 * task in order to keep the resources usage and the impact on other artifacts at a minimum.
 * <p>
 * The {@link Scheduler}s returned by methods in the implementations must provide access the the thread pools in the Mule Runtime
 * that have the expected tuning configuration.
 *
 * @since 1.0
 */
public interface SchedulerService extends Service {

  /**
   * Builds a fresh {@link Scheduler} for light CPU tasks. The returned {@link Scheduler} is backed by the Mule runtime cpu-light
   * executor.
   * <p>
   * A task is considered {@code cpu-light} if it doesn't block at any time and its duration is less than 10 milliseconds.
   * 
   * @return a scheduler for cpu-light tasks
   */
  Scheduler cpuLightScheduler();

  /**
   * Builds a fresh {@link Scheduler} for blocking I/O tasks. The returned {@link Scheduler} is backed by the Mule runtime
   * blocking I/O executor.
   * <p>
   * A task is considered {@code I/O} if it spends most of it's clock duration blocked due to I/O operations.
   * 
   * @return a scheduler for I/O tasks
   */
  Scheduler ioScheduler();

  /**
   * Builds a fresh {@link Scheduler} for heavy computation or CPU intensive tasks. The returned {@link Scheduler} is backed by
   * the Mule runtime computation executor.
   * <p>
   * A task is considered a {@code computation} if its duration is more than 10 milliseconds and less than 20% of its clock time
   * is due to blocking.
   * 
   * @return a scheduler for computation tasks
   */
  Scheduler computationScheduler();
}
