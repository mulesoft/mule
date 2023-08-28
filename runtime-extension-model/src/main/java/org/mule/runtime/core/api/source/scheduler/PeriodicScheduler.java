/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.source.scheduler;

import static java.util.Objects.requireNonNull;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.source.SchedulerConfiguration;

import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler for periodic jobs.
 *
 * @since 3.5.0, moved from {@link org.mule.runtime.core.api.schedule.SchedulerFactory}.
 */
@NoExtend
public abstract class PeriodicScheduler extends AbstractComponent implements Scheduler, SchedulerConfiguration {

  /**
   * {@inheritDoc}
   */
  @Override
  public final ScheduledFuture<?> schedule(org.mule.runtime.api.scheduler.Scheduler executor, Runnable job) {
    return requireNonNull(doSchedule(executor, job));
  }

  /**
   * Template method to delegate the scheduling of the job.
   *
   * @param executor the corresponding {@link org.mule.runtime.api.scheduler.Scheduler} instance.
   * @param job      The {@link Runnable} job that has to be executed.
   * @return the newly scheduled job.
   */
  protected abstract ScheduledFuture<?> doSchedule(org.mule.runtime.api.scheduler.Scheduler executor, Runnable job);

}
