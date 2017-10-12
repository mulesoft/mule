/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.scheduler;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import org.mule.runtime.api.source.SchedulerConfiguration;

import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler for periodic jobs.
 *
 * @since 3.5.0, moved from {@link org.mule.runtime.core.api.schedule.SchedulerFactory}.
 */
public abstract class PeriodicScheduler implements Scheduler, SchedulerConfiguration {

  /**
   * {@inheritDoc}
   */
  @Override
  public final ScheduledFuture<?> schedule(org.mule.runtime.api.scheduler.Scheduler executor, Runnable job) {
    ScheduledFuture<?> scheduler = doSchedule(executor, job);
    checkNull(scheduler);
    return scheduler;
  }

  /**
   * Template method to delegate the scheduling of the job.
   *
   * @param executor the corresponding {@link org.mule.runtime.api.scheduler.Scheduler} instance.
   * @param job The {@link Runnable} job that has to be executed.
   * @return the newly scheduled job.
   */
  protected abstract ScheduledFuture<?> doSchedule(org.mule.runtime.api.scheduler.Scheduler executor, Runnable job);

  private void checkNull(ScheduledFuture<?> postProcessedScheduler) {
    if (postProcessedScheduler == null) {
      throw new NullPointerException(objectIsNull("scheduler").toString());
    }
  }
}
