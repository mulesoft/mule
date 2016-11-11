/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Allows tasks to be submitted/scheduled to a specific executor in the Mule runtime. Different {@link Scheduler} instances may be
 * backed by the same {@link ExecutorService}, allowing for a fine control of the source of the tasks that the underlying
 * {@link ExecutorService} will run.
 * <p>
 * See {@link ScheduledExecutorService} and {@link ExecutorService} for documentation on the provided methods.
 * 
 * @since 4.0
 */
public interface Scheduler extends ScheduledExecutorService {

  /**
   * Creates and executes a periodic action that becomes enabled according to given {@code cronExpression} and {@code timeZone}.
   * If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only
   * terminate via cancellation or termination of the executor. If any execution of this task takes longer than the time before
   * the next execution should start, then subsequent executions may start late, but will not concurrently execute.
   *
   * @param command the task to execute
   * @param cronExpression the cron expression string to base the schedule on.
   * @return a ScheduledFuture representing pending completion of the task, and whose {@code get()} method will throw an exception
   *         upon cancellation
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @throws NullPointerException if command is null
   * @throws IllegalArgumentException if delay less than or equal to zero
   */
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression);

  /**
   * Creates and executes a periodic action that becomes enabled according to given {@code cronExpression} in the given
   * {@code timeZone}. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the
   * task will only terminate via cancellation or termination of the executor. If any execution of this task takes longer than the
   * time before the next execution should start, then subsequent executions may start late, but will not concurrently execute.
   *
   * @param command the task to execute
   * @param cronExpression the cron expression string to base the schedule on.
   * @param timeZone the time-zone for the schedule.
   * @return a ScheduledFuture representing pending completion of the task, and whose {@code get()} method will throw an exception
   *         upon cancellation
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @throws NullPointerException if command is null
   * @throws IllegalArgumentException if delay less than or equal to zero
   */
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone);

  /**
   * Tries to do a graceful shutdown.
   * <p>
   * If this hasn't terminated after a configured time, a forceful shutdown takes place.
   * 
   * @param gracefulShutdownTimeout the maximum time to wait for the running tasks to gracefully complete.
   * @param unit the time unit of the {@code timeout} argument
   */
  void stop(long gracefulShutdownTimeout, TimeUnit unit);
}
