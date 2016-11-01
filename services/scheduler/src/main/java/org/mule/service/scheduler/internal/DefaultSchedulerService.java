/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

/**
 * Default implementation of {@link SchedulerService}.
 * <p>
 * {@link Scheduler}s provided by this implementation of {@link SchedulerService} use a shared single-threaded
 * {@link ScheduledExecutorService} for scheduling work. When a scheduled tasks is fired, they are executed using the
 * {@link Scheduler}'s own executor.
 * <p>
 * The returned {@link Scheduler}s have an {@code AbortPolicy} rejection policy. That means that when sending a task to a full
 * {@link Scheduler} a {@link RejectedExecutionException} will be thrown.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

  private static final Logger logger = getLogger(DefaultSchedulerService.class);

  // TODO MULE-10585 Externalize this timeout
  private static final int GRACEFUL_SHUTDOWN_TIMEOUT_SECS = 60;

  private static final int TASK_QUEUE_SIZE = 2000;

  private static final String CPU_LIGHT_THREADS_NAME = SchedulerService.class.getSimpleName() + "_cpuLight";
  private static final String IO_THREADS_NAME = SchedulerService.class.getSimpleName() + "_io";
  private static final String COMPUTATION_THREADS_NAME = SchedulerService.class.getSimpleName() + "_compute";
  private static final String SCHEDULER_THREADS_NAME = SchedulerService.class.getSimpleName() + "_sched";

  private ExecutorService cpuLightExecutor;
  private ExecutorService ioExecutor;
  private ExecutorService computationExecutor;
  private ScheduledExecutorService scheduledExecutor;

  @Override
  public String getName() {
    return "SchedulerService";
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return new DefaultScheduler(cpuLightExecutor, scheduledExecutor);
  }

  @Override
  public Scheduler ioScheduler() {
    return new DefaultScheduler(ioExecutor, scheduledExecutor);
  }

  @Override
  public Scheduler computationScheduler() {
    return new DefaultScheduler(computationExecutor, scheduledExecutor);
  }

  @Override
  public void start() throws MuleException {
    int cores = getRuntime().availableProcessors();

    logger.info("Starting " + this.toString() + "...");

    // TODO MULE-10585 Externalize the threads configuration
    cpuLightExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(TASK_QUEUE_SIZE),
                                              new NamedThreadFactory(CPU_LIGHT_THREADS_NAME));
    ioExecutor = new ThreadPoolExecutor(cores, cores * cores, 0, SECONDS, new ArrayBlockingQueue<>(TASK_QUEUE_SIZE),
                                        new NamedThreadFactory(IO_THREADS_NAME));
    computationExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(TASK_QUEUE_SIZE),
                                                 new NamedThreadFactory(COMPUTATION_THREADS_NAME));
    scheduledExecutor = newScheduledThreadPool(1, new NamedThreadFactory(SCHEDULER_THREADS_NAME));

    ((ThreadPoolExecutor) cpuLightExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) ioExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) computationExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) scheduledExecutor).prestartAllCoreThreads();

    logger.info("Started " + this.toString());
  }

  @Override
  public void stop() throws MuleException {
    logger.info("Stopping " + this.toString() + "...");

    cpuLightExecutor.shutdown();
    ioExecutor.shutdown();
    computationExecutor.shutdown();
    scheduledExecutor.shutdown();

    try {
      final long startMillis = currentTimeMillis();

      // Stop the scheduled first to avoid it dispatching tasks to an already stopped executor
      waitForExecutorTermination(startMillis, scheduledExecutor, SCHEDULER_THREADS_NAME);
      waitForExecutorTermination(startMillis, cpuLightExecutor, CPU_LIGHT_THREADS_NAME);
      waitForExecutorTermination(startMillis, ioExecutor, IO_THREADS_NAME);
      waitForExecutorTermination(startMillis, computationExecutor, COMPUTATION_THREADS_NAME);

      logger.info("Stopped " + this.toString());
    } catch (InterruptedException e) {
      currentThread().interrupt();
      logger.info("Stop of " + this.toString() + " interrupted", e);
    }

    cpuLightExecutor = null;
    ioExecutor = null;
    computationExecutor = null;
    scheduledExecutor = null;
  }

  protected void waitForExecutorTermination(final long startMillis, final ExecutorService executor, final String executorLabel)
      throws InterruptedException {
    if (!executor.awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT_SECS * 1000 - (currentTimeMillis() - startMillis), MILLISECONDS)) {
      final List<Runnable> cancelledJobs = executor.shutdownNow();
      logger.warn("'" + executorLabel + "' " + executor.toString() + " of " + this.toString()
          + " did not shutdown gracefully after " + GRACEFUL_SHUTDOWN_TIMEOUT_SECS + " seconds. " + cancelledJobs.size()
          + " jobs were cancelled.");
    }
  }
}
