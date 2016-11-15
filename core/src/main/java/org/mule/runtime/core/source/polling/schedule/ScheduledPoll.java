/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling.schedule;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Definition of a Scheduler for poll.
 *
 * @since 3.5.0, moved from {@link org.mule.runtime.core.api.schedule.Scheduler}.
 */
public class ScheduledPoll implements Lifecycle, NameableObject {

  private Supplier<Scheduler> executorSupplier;
  private Consumer<Scheduler> executorStopper;

  /**
   * Thread executor service
   */
  private Scheduler executor;

  protected Runnable job;

  private final Function<Scheduler, ScheduledFuture<?>> jobScheduler;

  private ScheduledFuture<?> scheduledJob;

  /**
   * The {@link ScheduledPoll} name used as an identifier in the {@link org.mule.runtime.core.api.registry.MuleRegistry}.
   */
  protected String name;

  public ScheduledPoll(Supplier<Scheduler> executorSupplier, Consumer<Scheduler> executorStopper, String name, Runnable job,
                       Function<Scheduler, ScheduledFuture<?>> jobScheduler) {
    this.executorSupplier = executorSupplier;
    this.executorStopper = executorStopper;
    this.name = name;
    this.job = job;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public void initialise() throws InitialisationException {
    executor = executorSupplier.get();
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
    executorStopper.accept(executor);
    executor = null;
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
