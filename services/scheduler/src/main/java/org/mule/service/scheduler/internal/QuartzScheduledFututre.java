/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class QuartzScheduledFututre<V> implements ScheduledFuture<V> {

  private Scheduler quartzScheduler;
  private Trigger trigger;
  private RunnableFuture<?> task;

  /**
   * 
   * @param task the actual task that was scheduled.
   */
  QuartzScheduledFututre(org.quartz.Scheduler quartzScheduler, Trigger trigger, RunnableFuture<?> task) {
    this.quartzScheduler = quartzScheduler;
    this.trigger = trigger;
    this.task = task;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(trigger.getNextFireTime().toInstant().getNano() - nanoTime(), NANOSECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    long diff = getDelay(NANOSECONDS) - o.getDelay(NANOSECONDS);
    return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean quartzCancelled;
    try {
      quartzCancelled = quartzScheduler.unscheduleJob(trigger.getKey());
    } catch (SchedulerException e) {
      throw new MuleRuntimeException(e);
    }
    boolean taskCancelled = task.cancel(mayInterruptIfRunning);
    return quartzCancelled || taskCancelled;
  }

  @Override
  public boolean isCancelled() {
    return task.isCancelled();
  }

  @Override
  public boolean isDone() {
    return task.isCancelled() || task.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return (V) task.get();
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return (V) task.get(timeout, unit);
  }

}
