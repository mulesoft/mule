/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.lineSeparator;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.TimeZone.getDefault;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.service.scheduler.internal.QuartzCronJob.JOB_TASK_KEY;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy for a {@link ScheduledExecutorService} that adds tracking of the source of the dispatched tasks.
 *
 * @since 4.0
 */
class DefaultScheduler extends AbstractExecutorService implements Scheduler {

  /**
   * Forced shutdown delay. The time to wait while threads are being interrupted.
   */
  private static final long FORCEFUL_SHUTDOWN_TIMEOUT_SECS = 5;

  private static final Logger logger = LoggerFactory.getLogger(DefaultScheduler.class);

  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutor;
  private final org.quartz.Scheduler quartzScheduler;

  private Class<? extends QuartzCronJob> jobClass = QuartzCronJob.class;

  /**
   * Wait condition to support awaitTermination
   */
  private final CountDownLatch terminationLatch = new CountDownLatch(1);

  private static final ScheduledFuture<?> NULL_SCHEDULED_FUTURE = NullScheduledFuture.INSTANCE;
  private Map<RunnableFuture<?>, ScheduledFuture<?>> scheduledTasks;
  private Set<RunnableFuture<?>> cancelledBeforeFireTasks;

  private volatile boolean shutdown = false;

  /**
   * @param executor the actual executor that will run the dispatched tasks.
   * @param workers an estimate of how many threads will be, at maximum, in the underlying executor
   * @param totalWorkers an estimate of how many threads will be, at maximum, in all the underlying executors
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   */
  DefaultScheduler(ExecutorService executor, int workers, int totalWorkers, ScheduledExecutorService scheduledExecutor,
                   org.quartz.Scheduler quartzScheduler) {
    scheduledTasks = new ConcurrentHashMap<>(workers, 1.00f, totalWorkers);
    cancelledBeforeFireTasks = newKeySet();
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
    this.quartzScheduler = quartzScheduler;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = newTaskFor(command, null);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(callable);

    final RunnableFuture<V> task = newTaskFor(callable);

    final ScheduledFuture<V> scheduled =
        new ScheduledFutureDecorator(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = new RunnableRepeatableFutureDecorator<>(() -> super.newTaskFor(command, null), t -> {
      if (t.isCancelled()) {
        taskFinished(t);
      }
    }, this);

    final ScheduledFuture<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.scheduleAtFixedRate(schedulableTask(task), initialDelay, period, unit),
                                       task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = new RunnableRepeatableFutureDecorator<>(() -> super.newTaskFor(command, null), t -> {
      if (!t.isCancelled()) {
        scheduledExecutor.schedule(schedulableTask(t), delay, unit);
      } else {
        taskFinished(t);
      }
    }, this);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.schedule(schedulableTask(task), initialDelay, unit), task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    return scheduleWithCronExpression(command, cronExpression, getDefault());
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = new RunnableRepeatableFutureDecorator<>(() -> super.newTaskFor(command, null), t -> {
      if (t.isCancelled()) {
        taskFinished(t);
      }
    }, this);

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(JOB_TASK_KEY, schedulableTask(task));
    JobDetail job = newJob(jobClass).usingJobData(jobDataMap).build();

    CronTrigger trigger = newTrigger().withSchedule(cronSchedule(cronExpression).inTimeZone(timeZone)).build();

    try {
      quartzScheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      throw new MuleRuntimeException(e);
    }

    QuartzScheduledFututre<Object> scheduled = new QuartzScheduledFututre<>(quartzScheduler, trigger, task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  private <T> Runnable schedulableTask(RunnableFuture<T> task) {
    return () -> executor.execute(task);
  }

  public void setJobClass(Class<? extends QuartzCronJob> jobClass) {
    this.jobClass = jobClass;
  }

  @Override
  public void shutdown() {
    logger.debug("Shutting down " + this.toString());
    this.shutdown = true;
    tryTerminate();
  }

  @Override
  public List<Runnable> shutdownNow() {
    logger.debug("Shutting down NOW " + this.toString());
    shutdown();

    List<Runnable> tasks;
    try {
      tasks = new ArrayList<>(scheduledTasks.size() + cancelledBeforeFireTasks.size());
      tasks.addAll(cancelledBeforeFireTasks);

      for (Entry<RunnableFuture<?>, ScheduledFuture<?>> taskEntry : scheduledTasks.entrySet()) {
        taskEntry.getValue().cancel(true);
        taskEntry.getKey().cancel(true);
        if (taskEntry.getKey() instanceof RunnableFutureDecorator
            && !((RunnableFutureDecorator<?>) taskEntry.getKey()).isStarted()) {
          tasks.add(taskEntry.getKey());
        }
      }
      scheduledTasks.clear();
      cancelledBeforeFireTasks.clear();

      return tasks;
    } finally {
      tryTerminate();
    }
  }

  @Override
  public boolean isShutdown() {
    return shutdown || executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return shutdown && scheduledTasks.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    final long stopNanos = nanoTime() + unit.toNanos(timeout);
    while (nanoTime() <= stopNanos) {
      if (isTerminated()) {
        return true;
      }
      // Do this in a while just in case that the termination occurred right before this next line is executed.
      if (terminationLatch.await(50, MILLISECONDS)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void stop(long gracefulShutdownTimeout, TimeUnit unit) {
    // Disable new tasks from being submitted
    shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!awaitTermination(gracefulShutdownTimeout, unit)) {
        // Cancel currently executing tasks and return list of pending
        // tasks
        List<Runnable> cancelledJobs = shutdownNow();
        // Wait a while for tasks to respond to being cancelled
        if (!awaitTermination(FORCEFUL_SHUTDOWN_TIMEOUT_SECS, SECONDS)) {
          logger.warn("Scheduler " + this.toString() + " did not shutdown gracefully after " + gracefulShutdownTimeout
              + " " + unit.toString() + ". " + cancelledJobs.size() + " jobs were cancelled.");
        } else {
          if (!cancelledJobs.isEmpty()) {
            logger.warn("Scheduler " + this.toString() + " terminated. " + cancelledJobs.size() + " jobs were cancelled.");
          }
        }
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      shutdownNow();
      // Preserve interrupt status
      currentThread().interrupt();
    }
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new RunnableFutureDecorator<>(super.newTaskFor(callable), this);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new RunnableFutureDecorator<>(super.newTaskFor(runnable, value), this);
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();
    if (command instanceof RunnableFuture) {
      scheduledTasks.put((RunnableFuture<?>) command, NULL_SCHEDULED_FUTURE);
    } else {
      scheduledTasks.put(newTaskFor(command, null), NULL_SCHEDULED_FUTURE);
    }

    executor.execute(command);
  }

  protected void checkShutdown() {
    if (isShutdown()) {
      throw new RejectedExecutionException(this.toString() + " already shutdown");
    }
  }

  protected void taskFinished(RunnableFuture<?> task) {
    scheduledTasks.remove(task);
    if (task instanceof AbstractRunnableFutureDecorator && !((AbstractRunnableFutureDecorator) task).isStarted()) {
      cancelledBeforeFireTasks.add(task);
    }
    tryTerminate();
  }

  private void tryTerminate() {
    if (isTerminated()) {
      terminationLatch.countDown();
    }
  }

  @Override
  public String toString() {
    return super.toString() + "{" + lineSeparator()
        + "  executor: " + executor.toString() + lineSeparator()
        + "  shutdown: " + shutdown + lineSeparator()
        + "}";
  }
}
