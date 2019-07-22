/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.quartz.impl.triggers.CronTriggerImpl;

public class RunNowCronSchedulerWrapper extends PeriodicScheduler {

  private CronScheduler cronScheduler;

  public RunNowCronSchedulerWrapper(CronScheduler scheduler) {
    this.cronScheduler = scheduler;
  }

  @Override
  protected ScheduledFuture<?> doSchedule(Scheduler executor, Runnable job) {
    String expression = cronScheduler.getExpression();
    TimeZone timezone = cronScheduler.resolveTimeZone(cronScheduler.getTimeZone());
    try {
      CronTriggerImpl trigger =
          new CronTriggerImpl("CheckTrigger", "TestGroup", "CheckExpression", "TestJobGroup", expression, timezone);
      if (!trigger.willFireOn(Calendar.getInstance(timezone))) {
        System.out.println("Adding an extra run.");
        executor.schedule(job, 0, TimeUnit.SECONDS);
      }
    } catch (ParseException e) {
      throw new MuleRuntimeException(e);
    }
    return cronScheduler.doSchedule(executor, job);
  }
}
