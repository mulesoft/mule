/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.source.FixedFrequencySchedulerConfiguration;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link PeriodicScheduler} for a fixed-frequency job.
 */
@Alias("fixed-frequency")
public class FixedFrequencyScheduler extends PeriodicScheduler implements FixedFrequencySchedulerConfiguration {

  @Parameter
  @Optional(defaultValue = "MILLISECONDS")
  private TimeUnit timeUnit = MILLISECONDS;

  @Parameter
  @Optional(defaultValue = "1000")
  private long frequency = 1000l;

  @Parameter
  @Optional(defaultValue = "0")
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

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public long getFrequency() {
    return frequency;
  }

  public long getStartDelay() {
    return startDelay;
  }
}
