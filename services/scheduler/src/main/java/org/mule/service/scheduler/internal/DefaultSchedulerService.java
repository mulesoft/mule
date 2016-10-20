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

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link SchedulerService}.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

  private static final Logger logger = LoggerFactory.getLogger(DefaultSchedulerService.class);

  private static final int GRACEFUL_SHUTDOWN_TIMEOUT_SECS = 60;

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
    return new DefaultScheduler(cpuLightExecutor, scheduledExecutor, true);
  }

  @Override
  public Scheduler ioScheduler() {
    return new DefaultScheduler(ioExecutor, scheduledExecutor, false);
  }

  @Override
  public Scheduler computationScheduler() {
    return new DefaultScheduler(computationExecutor, scheduledExecutor, false);
  }

  @Override
  public void start() throws MuleException {
    int cores = getRuntime().availableProcessors();

    logger.info("Starting " + this.toString() + "...");

    final String prefix = SchedulerService.class.getSimpleName();
    cpuLightExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(2 * cores),
                                              new NamedThreadFactory(prefix + "_cpuLight"));
    ioExecutor = new ThreadPoolExecutor(cores, cores * cores, 0, SECONDS, new ArrayBlockingQueue<>(cores * cores),
                                        new NamedThreadFactory(prefix + "_io"));
    computationExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(2 * cores),
                                                 new NamedThreadFactory(prefix + "_compute"));
    scheduledExecutor = newScheduledThreadPool(1, new NamedThreadFactory(prefix + "_sched"));

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
      waitForExecutorTermination(startMillis, scheduledExecutor, "scheduledExecutor");
      waitForExecutorTermination(startMillis, cpuLightExecutor, "cpuLightExecutor");
      waitForExecutorTermination(startMillis, ioExecutor, "ioExecutor");
      waitForExecutorTermination(startMillis, computationExecutor, "computationExecutor");

      logger.info("Stopped " + this.toString());
    } catch (InterruptedException e) {
      currentThread().interrupt();
      logger.info("Stop of " + this.toString() + " interrupted", e);
    }
  }

  protected void waitForExecutorTermination(final long startMillis, final ExecutorService executor, final String executorLabel)
      throws InterruptedException {
    if (!executor.awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT_SECS * 1000 - (currentTimeMillis() - startMillis), MILLISECONDS)) {
      final List<Runnable> cancelledJobs = scheduledExecutor.shutdownNow();
      logger.warn("'" + executorLabel + "' " + scheduledExecutor.toString() + " of " + this.toString()
          + " did not shutdown gracefully after " + GRACEFUL_SHUTDOWN_TIMEOUT_SECS + " seconds. " + cancelledJobs.size()
          + " jobs were cancelled.");
    }
  }
}
