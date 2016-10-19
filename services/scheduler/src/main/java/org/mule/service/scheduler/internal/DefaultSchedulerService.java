/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Default implementation of {@link SchedulerService}.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

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

    final String prefix = SchedulerService.class.getSimpleName();
    cpuLightExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(2 * cores),
                                              new NamedThreadFactory(prefix + "_cpuLight"));
    ioExecutor = new ThreadPoolExecutor(cores, cores * cores, 0, SECONDS, new ArrayBlockingQueue<>(cores * cores),
                                        new NamedThreadFactory(prefix + "_io"));
    computationExecutor = new ThreadPoolExecutor(2 * cores, 2 * cores, 0, SECONDS, new ArrayBlockingQueue<>(2 * cores),
                                                 new NamedThreadFactory(prefix + "_compute"));
    scheduledExecutor = newScheduledThreadPool(1, new NamedThreadFactory(prefix + "_sched"));
  }

  @Override
  public void stop() throws MuleException {
    cpuLightExecutor.shutdown();
    ioExecutor.shutdown();
    computationExecutor.shutdown();
    scheduledExecutor.shutdown();
  }
}
