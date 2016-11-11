/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling.schedule;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;


/**
 * <p>
 * Abstract definition of a Scheduler for poll.
 * </p>
 *
 * @since 3.5.0
 */
public class ScheduledPoll implements Lifecycle, NameableObject {

  /**
   * <p>
   * Thread executor service
   * </p>
   */
  private Scheduler executor;

  protected Runnable job;

  private final SchedulerService schedulerService;

  private final Function<Scheduler, ScheduledFuture<?>> jobScheduler;

  private ScheduledFuture<?> scheduledJob;

  /**
   * <p>
   * The {@link org.mule.runtime.core.api.schedule.Scheduler} name used as an identifier in the
   * {@link org.mule.runtime.core.api.registry.MuleRegistry}
   * </p>
   */
  protected String name;

  public ScheduledPoll(SchedulerService schedulerService, String name, Runnable job,
                       Function<Scheduler, ScheduledFuture<?>> jobScheduler) {
    this.schedulerService = schedulerService;
    this.name = name;
    this.job = job;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public void initialise() throws InitialisationException {
    // TODO Allow to configure the type of task to do
    executor = schedulerService.ioScheduler();
  }

  @Override
  public void start() throws MuleException {
    scheduledJob = jobScheduler.apply(executor);
  }

  @Override
  public void stop() throws MuleException {
    if (scheduledJob != null) {
      scheduledJob.cancel(false);
    }
  }

  @Override
  public void dispose() {
    executor.stop(1000, MILLISECONDS);
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
