/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RunNowCronSchedulerWrapper extends PeriodicScheduler {

  private CronScheduler cronScheduler;

  public RunNowCronSchedulerWrapper(CronScheduler scheduler) {
    this.cronScheduler = scheduler;
  }

  @Override
  protected ScheduledFuture<?> doSchedule(Scheduler executor, Runnable job) {
    ScheduledFuture<?> future = executor.schedule(job, 0, TimeUnit.SECONDS);
    return cronScheduler.doSchedule(executor, job);
  }
}
