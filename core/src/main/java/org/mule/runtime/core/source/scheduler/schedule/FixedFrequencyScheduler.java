/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.scheduler.schedule;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.source.polling.PeriodicScheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link PeriodicScheduler} for a fixed-frequency job.
 */
public class FixedFrequencyScheduler extends PeriodicScheduler {

  /**
   * The {@link TimeUnit} of the scheduler
   */
  private TimeUnit timeUnit = MILLISECONDS;

  /**
   * The frequency of the scheduler in timeUnit
   */
  private long frequency = 1000l;

  /**
   * The time in timeUnit that it has to wait before executing the first task
   */
  private long startDelay = 1000l;


  @Override
  public ScheduledFuture<?> doSchedule(Scheduler executor, Runnable job) {
    return executor.scheduleAtFixedRate(job, startDelay, frequency, timeUnit);
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public void setFrequency(long frequency) {
    checkArgument(frequency > 0, "Frequency must be greater then zero");

    this.frequency = frequency;
  }

  public void setStartDelay(long startDelay) {
    checkArgument(startDelay >= 0, "Start delay must be greater then zero");

    this.startDelay = startDelay;
  }


}
