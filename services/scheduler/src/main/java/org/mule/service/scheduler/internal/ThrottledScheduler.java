/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.service.scheduler.ThreadType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Extension of {@link DefaultScheduler} that has a limit on the tasks that can be run at the same time.
 * <p>
 * Exceeding tasks will block the caller, until a running task is finished.
 * 
 * @since 4.0
 */
public class ThrottledScheduler extends DefaultScheduler {

  private final int maxConcurrentTasks;
  private final AtomicInteger runningTasks = new AtomicInteger();

  /**
   * @param name the name of this scheduler
   * @param executor the actual executor that will run the dispatched tasks.
   * @param workers an estimate of how many threads will be, at maximum, in the underlying executor
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   * @param quartzScheduler the quartz object that will handle tasks scheduled with cron expressions. This will not execute the
   *        actual tasks, but will dispatch it to the {@code executor} at the appropriate time.
   * @param threadsType The {@link ThreadType} that matches with the {@link Thread}s managed by this {@link Scheduler}.
   * @param maxConcurrentTasks how many tasks can be running at the same time for this {@link Scheduler}.
   * @param shutdownCallback a callback to be invoked when this scheduler is stopped/shutdown.
   */
  ThrottledScheduler(String name, ExecutorService executor, int workers, ScheduledExecutorService scheduledExecutor,
                     org.quartz.Scheduler quartzScheduler, ThreadType threadsType, int maxConcurrentTasks,
                     Consumer<Scheduler> shutdownCallback) {
    super(name, executor, workers, scheduledExecutor, quartzScheduler, threadsType, shutdownCallback);
    this.maxConcurrentTasks = maxConcurrentTasks;
  }

  @Override
  protected void putTask(RunnableFuture<?> task, ScheduledFuture<?> scheduledFuture) {
    try {
      synchronized (runningTasks) {
        runningTasks.incrementAndGet();
        while (runningTasks.get() >= maxConcurrentTasks) {
          runningTasks.wait();
        }
      }

      super.putTask(task, scheduledFuture);
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected void removeTask(RunnableFuture<?> task) {
    super.removeTask(task);

    synchronized (runningTasks) {
      runningTasks.decrementAndGet();
      runningTasks.notify();
    }
  }

  @Override
  public String toString() {
    return super.toString() + " (throttled: " + runningTasks.get() + "/" + maxConcurrentTasks + ")";
  }
}
