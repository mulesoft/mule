/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.api.service.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Provides access to the different schedulers and thread pools that exist in the Mule runtime, allowing an artifact to schedule
 * tasks on those.
 * <p>
 * This interface provides access to different schedulers, each with its own configured tuning options, optimized for some
 * specific kind of work. Artifacts that need to dispatch tasks need to carefully determine which scheduler best fits the nature
 * of the task in order to keep the resources usage and the impact on other artifacts at a minimum.
 * <p>
 * The {@link Scheduler}s returned by methods in the implementations must provide access the the thread pools in the Mule Runtime
 * that have the expected tuning configuration. Each {@link Scheduler} will have its own lifecycle, managed by its user and NOT
 * this service.
 *
 * @since 4.0
 */
public interface SchedulerService extends Service {

  /**
   * Builds a fresh {@link Scheduler} with a default configuration for light CPU tasks. The returned {@link Scheduler} is backed
   * by the Mule runtime cpu-light executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code cpu-light} if it doesn't block at any time and its duration is less than 10 milliseconds.
   * <p>
   * Implementations must get the appropriate config from the runtime context to build the target {@link Scheduler}. This method
   * must act only as a delegate of {@link #cpuLightScheduler(SchedulerConfig, SchedulerPoolsConfigFactory)}.
   *
   * @return a scheduler that runs {@code cpu-light} tasks.
   */
  Scheduler cpuLightScheduler();

  /**
   * Builds a fresh {@link Scheduler} with a default configuration for blocking I/O tasks. The returned {@link Scheduler} is
   * backed by the Mule runtime blocking I/O executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code blocking I/O} if it spends most of it's clock duration blocked due to I/O operations.
   * <p>
   * Implementations must get the appropriate config from the runtime context to build the target {@link Scheduler}. This method
   * must act only as a delegate of {@link #ioScheduler(SchedulerConfig, SchedulerPoolsConfigFactory)}.
   *
   * @return a scheduler that runs {@code blocking I/O} tasks.
   */
  Scheduler ioScheduler();

  /**
   * Builds a fresh {@link Scheduler} with a default configuration for heavy computation or CPU intensive tasks. The returned
   * {@link Scheduler} is backed by the Mule runtime computation executor, which is shared by all {@link Scheduler}s returned by
   * this method.
   * <p>
   * A task is considered a {@code CPU intensive} if its duration is more than 10 milliseconds and less than 20% of its clock time
   * is due to blocking.
   * <p>
   * Implementations must get the appropriate config from the runtime context to build the target {@link Scheduler}. This method
   * must act only as a delegate of {@link #cpuIntensiveScheduler(SchedulerConfig, SchedulerPoolsConfigFactory)}.
   *
   * @return a scheduler that runs {@code CPU intensive} tasks.
   */
  Scheduler cpuIntensiveScheduler();

  /**
   * Builds a fresh {@link Scheduler} for light CPU tasks. The returned {@link Scheduler} is backed by the Mule runtime cpu-light
   * executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code cpu-light} if it doesn't block at any time and its duration is less than 10 milliseconds.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   *
   * @return a scheduler that runs {@code cpu-light} tasks.
   */
  Scheduler cpuLightScheduler(SchedulerConfig config);

  /**
   * Builds a fresh {@link Scheduler} for blocking I/O tasks. The returned {@link Scheduler} is backed by the Mule runtime
   * blocking I/O executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code blocking I/O} if it spends most of it's clock duration blocked due to I/O operations.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   *
   * @return a scheduler that runs {@code blocking I/O} tasks.
   */
  Scheduler ioScheduler(SchedulerConfig config);

  /**
   * Builds a fresh {@link Scheduler} for heavy computation or CPU intensive tasks. The returned {@link Scheduler} is backed by
   * the Mule runtime computation executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered a {@code CPU intensive} if its duration is more than 10 milliseconds and less than 20% of its clock time
   * is due to blocking.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   *
   * @return a scheduler that runs {@code CPU intensive} tasks.
   */
  Scheduler cpuIntensiveScheduler(SchedulerConfig config);

  /**
   * Builds a fresh {@link Scheduler} for light CPU tasks. The returned {@link Scheduler} is backed by the Mule runtime cpu-light
   * executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code cpu-light} if it doesn't block at any time and its duration is less than 10 milliseconds.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   * @param poolsConfigFactory the configuration to use for the thread pools that the schedulers use.
   *
   * @return a scheduler that runs {@code cpu-light} tasks.
   */
  Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory);

  /**
   * Builds a fresh {@link Scheduler} for blocking I/O tasks. The returned {@link Scheduler} is backed by the Mule runtime
   * blocking I/O executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered {@code blocking I/O} if it spends most of it's clock duration blocked due to I/O operations.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   * @param poolsConfigFactory the configuration to use for the thread pools that the schedulers use.
   *
   * @return a scheduler that runs {@code blocking I/O} tasks.
   */
  Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory);

  /**
   * Builds a fresh {@link Scheduler} for heavy computation or CPU intensive tasks. The returned {@link Scheduler} is backed by
   * the Mule runtime computation executor, which is shared by all {@link Scheduler}s returned by this method.
   * <p>
   * A task is considered a {@code CPU intensive} if its duration is more than 10 milliseconds and less than 20% of its clock time
   * is due to blocking.
   * <p>
   * If the provided {@code config} has {@code maxConcurrentTasks} set, exceeding tasks will block the caller, until a running
   * task is finished.
   *
   * @param config allows customization of the returned scheduler.
   * @param poolsConfigFactory the configuration to use for the thread pools that the schedulers use.
   *
   * @return a scheduler that runs {@code CPU intensive} tasks.
   */
  Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory);

  /**
   * Builds a fresh {@link Scheduler} for custom tasks. The returned {@link Scheduler} is backed by an
   * {@link java.util.concurrent.ExecutorService} built with the given {@code corePoolSize} threads and a
   * {@link SynchronousQueue}.
   *
   * @param config allows customization of the returned scheduler.
   * @return a scheduler whose threads manage {@code custom} tasks.
   */
  Scheduler customScheduler(SchedulerConfig config);

  /**
   * Builds a fresh {@link Scheduler} for custom tasks. The returned {@link Scheduler} is backed by an
   * {@link java.util.concurrent.ExecutorService} built with the given {@code corePoolSize} threads and a
   * {@link LinkedBlockingQueue} with the given {@code queueSize}.
   *
   * @param config allows customization of the returned scheduler.
   * @return a scheduler whose threads manage {@code custom} tasks.
   */
  Scheduler customScheduler(SchedulerConfig config, int queueSize);

  /**
   * Provides a read-only view of all currently active {@link Scheduler}s created through this service.
   *
   * @return a {@link List} of {@link SchedulerView}s for all currently active {@link Scheduler}s.
   */
  List<SchedulerView> getSchedulers();

}
