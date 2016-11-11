/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides a thread-pool to be used by the {@link MuleClient} when running in standalone mode.
 *
 * @since 4.0
 */
class StandaloneClientSchedulerService implements SchedulerService, Startable, Stoppable {

  private Scheduler scheduler;

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void start() throws MuleException {
    scheduler = new StandaloneClientThreadScheduler();
  }

  @Override
  public void stop() throws MuleException {
    scheduler.stop(5, SECONDS);
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return scheduler;
  }

  @Override
  public Scheduler ioScheduler() {
    return scheduler;
  }

  @Override
  public Scheduler computationScheduler() {
    return scheduler;
  }

  @Override
  public boolean isCurrentThreadCpuLight() {
    return false;
  }

  @Override
  public boolean isCurrentThreadIo() {
    return false;
  }

  @Override
  public boolean isCurrentThreadComputation() {
    return false;
  }

  private static class StandaloneClientThreadScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    public StandaloneClientThreadScheduler() {
      super(getRuntime().availableProcessors() * 2);
    }

    @Override
    public void stop(long gracefulShutdownTimeoutSecs, TimeUnit unit) {
      shutdownNow();
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
      throw new UnsupportedOperationException("Cron scheduling is not supported in standalone.");
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
      throw new UnsupportedOperationException("Cron scheduling is not supported in standalone.");
    }
  }

}
