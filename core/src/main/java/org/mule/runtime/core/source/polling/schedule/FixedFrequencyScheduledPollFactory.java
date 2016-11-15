/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling.schedule;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.source.polling.ScheduledPollFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * Implementation of {@link ScheduledPollFactory} for a fixed-frequency {@link ScheduledPoll}.
 * </p>
 *
 * @since 3.5.0
 */
public class FixedFrequencyScheduledPollFactory extends ScheduledPollFactory {

  /**
   * <p>
   * The {@link TimeUnit} of the scheduler
   * </p>
   */
  private TimeUnit timeUnit = MILLISECONDS;

  /**
   * <p>
   * The frequency of the scheduler in timeUnit
   * </p>
   */
  private long frequency = 1000l;

  /**
   * <p>
   * The time in timeUnit that it has to wait before executing the first task
   * </p>
   */
  private long startDelay = 1000l;


  @Override
  public ScheduledPoll doCreate(Supplier<Scheduler> executorSupplier, Consumer<Scheduler> executorStopper, String name,
                                final Runnable job) {
    return new ScheduledPoll(executorSupplier, executorStopper, name, job,
                             executor -> executor.scheduleAtFixedRate(job, startDelay, frequency, timeUnit));
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
