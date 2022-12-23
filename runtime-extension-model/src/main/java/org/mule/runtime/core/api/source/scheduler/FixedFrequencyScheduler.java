/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;


import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.internal.dsl.DslConstants.FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.source.FixedFrequencySchedulerConfiguration;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link PeriodicScheduler} for a fixed-frequency job.
 */
@Alias(FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER)
public final class FixedFrequencyScheduler extends PeriodicScheduler implements FixedFrequencySchedulerConfiguration {

  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "MILLISECONDS")
  private TimeUnit timeUnit = MILLISECONDS;

  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "60000")
  private long frequency = 60000l;

  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "0")
  private long startDelay = 1000l;

  public FixedFrequencyScheduler() {
  }

  /**
   * Creates a new instance
   *
   * @since 4.6.0
   */
  public FixedFrequencyScheduler(long frequency, long startDelay, TimeUnit timeUnit) {
    setFrequency(frequency);
    setStartDelay(startDelay);
    setTimeUnit(timeUnit);
  }

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

  @Override
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  @Override
  public long getFrequency() {
    return frequency;
  }

  @Override
  public long getStartDelay() {
    return startDelay;
  }
}
