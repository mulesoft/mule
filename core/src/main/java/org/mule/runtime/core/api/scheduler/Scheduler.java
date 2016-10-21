/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
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
   * Tries to do a graceful shutdown.
   * <p>
   * If this hasn't terminated after a configured time, a forceful shutdown takes place.
   * 
   * @param gracefulShutdownTimeout the maximum time to wait for the running tasks to gracefully complete.
   * @param unit the time unit of the {@code timeout} argument
   */
  void stop(long gracefulShutdownTimeout, TimeUnit unit);
}
