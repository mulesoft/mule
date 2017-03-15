/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Integer.toHexString;
import static java.lang.Runtime.getRuntime;
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
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.service.scheduler.ThreadType;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

  private final AtomicInteger idGenerator = new AtomicInteger(0);

  private final String name;

  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutor;
  private final org.quartz.Scheduler quartzScheduler;

  private final ThreadType threadType;

  private Class<? extends QuartzCronJob> jobClass = QuartzCronJob.class;

  /**
   * Wait condition to support awaitTermination
   */
  private final CountDownLatch terminationLatch = new CountDownLatch(1);

  private static final ScheduledFuture<?> NULL_SCHEDULED_FUTURE = NullScheduledFuture.INSTANCE;
  private Map<RunnableFuture<?>, ScheduledFuture<?>> scheduledTasks;
  private Set<RunnableFuture<?>> cancelledBeforeFireTasks;

  private volatile boolean shutdown = false;

  private Consumer<Scheduler> shutdownCallback;

  /**
   * @param name the name of this scheduler
   * @param executor the actual executor that will run the dispatched tasks.
   * @param workers an estimate of how many threads will be, at maximum, in the underlying executor
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   * @param quartzScheduler the quartz object that will handle tasks scheduled with cron expressions. This will not execute the
   *        actual tasks, but will dispatch it to the {@code executor} at the appropriate time.
   * @param threadsType The {@link ThreadType} that matches with the {@link Thread}s managed by this {@link Scheduler}.
   * @param EMPTY_SHUTDOWN_CALLBACK a callback to be invoked when this scheduler is stopped/shutdown.
   */
  DefaultScheduler(String name, ExecutorService executor, int workers, ScheduledExecutorService scheduledExecutor,
                   org.quartz.Scheduler quartzScheduler, ThreadType threadsType, Consumer<Scheduler> shutdownCallback) {
    this.name = name + "@" + toHexString(hashCode());
    scheduledTasks = new ConcurrentHashMap<>(workers, 1.00f, getRuntime().availableProcessors());
    cancelledBeforeFireTasks = newKeySet();
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
    this.quartzScheduler = quartzScheduler;
    this.threadType = threadsType;
    this.shutdownCallback = shutdownCallback;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = newTaskFor(command, null);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    putTask(task, scheduled);
    return scheduled;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(callable);

    final RunnableFuture<V> task = newTaskFor(callable);

    final ScheduledFuture<V> scheduled =
        new ScheduledFutureDecorator(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    putTask(task, scheduled);
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
    }, currentThread().getContextClassLoader(), this, command.getClass().getName(), idGenerator.getAndIncrement());

    final ScheduledFuture<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.scheduleAtFixedRate(schedulableTask(task), initialDelay, period, unit),
                                       task);

    putTask(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFuture<?> task = new RunnableRepeatableFutureDecorator<>(() -> super.newTaskFor(command, null), t -> {
      fixedDelayWrapUp(t, delay, unit);
    }, currentThread().getContextClassLoader(), this, command.getClass().getName(), idGenerator.getAndIncrement());

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.schedule(reschedulableTask(task, delay, unit), initialDelay, unit),
                                       task);

    putTask(task, scheduled);
    return scheduled;
  }

  private <T> Runnable reschedulableTask(RunnableFuture<T> task, long delay, TimeUnit unit) {
    return () -> {
      try {
        executor.execute(task);
      } catch (RejectedExecutionException e) {
        // Just log. Do not rethrow so the periodic job is not cancelled
        logger.warn(e.getClass().getName() + " scheduling next execution of task " + task.toString() + ". Message was: "
            + e.getMessage());

        fixedDelayWrapUp(task, delay, unit);
      }
    };
  }

  private void fixedDelayWrapUp(RunnableFuture<?> task, long delay, TimeUnit unit) {
    if (!task.isCancelled()) {
      scheduledExecutor.schedule(reschedulableTask(task, delay, unit), delay, unit);
    } else {
      taskFinished(task);
    }
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
    }, currentThread().getContextClassLoader(), this, command.getClass().getName(), idGenerator.getAndIncrement());

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

    putTask(task, scheduled);
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
    shutdownCallback.accept(this);
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
    return isTerminated();
  }

  @Override
  public void stop(long gracefulShutdownTimeout, TimeUnit unit) {
    // Disable new tasks from being submitted
    shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!awaitTermination(gracefulShutdownTimeout, unit)) {
        // Cancel currently executing tasks and return list of pending tasks
        List<Runnable> cancelledJobs = shutdownNow();
        // Wait a while for tasks to respond to being cancelled
        if (!awaitTermination(FORCEFUL_SHUTDOWN_TIMEOUT_SECS, SECONDS)) {
          logger.warn("Scheduler " + this.toString() + " did not shutdown gracefully after " + gracefulShutdownTimeout
              + " " + unit.toString() + ".");
        } else {
          if (!cancelledJobs.isEmpty()) {
            logger.warn("Scheduler " + this.toString() + " terminated.");
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("The jobs " + cancelledJobs + " were cancelled.");
        } else {
          logger.info(cancelledJobs.size() + " jobs were cancelled.");
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
    return newDecoratedTaskFor(super.newTaskFor(callable), callable.getClass());
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return newDecoratedTaskFor(super.newTaskFor(runnable, value), runnable.getClass());
  }

  private <T> RunnableFuture<T> newDecoratedTaskFor(final RunnableFuture<T> newTaskFor, final Class<?> taskClass) {
    return new RunnableFutureDecorator<>(newTaskFor, currentThread().getContextClassLoader(), this, taskClass.getName(),
                                         idGenerator.getAndIncrement());
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();

    RunnableFuture<Object> runnableFutureCommand;
    if (command instanceof RunnableFuture) {
      runnableFutureCommand = (RunnableFuture<Object>) command;
    } else {
      runnableFutureCommand = newTaskFor(command, null);
    }

    putTask(runnableFutureCommand, NULL_SCHEDULED_FUTURE);
    try {
      executor.execute(runnableFutureCommand);
    } catch (Exception e) {
      removeTask(runnableFutureCommand);
      throw e;
    }
  }

  protected void checkShutdown() {
    if (isShutdown()) {
      throw new RejectedExecutionException(this.toString() + " already shutdown");
    }
  }

  protected void taskFinished(RunnableFuture<?> task) {
    removeTask(task);
    if (task instanceof AbstractRunnableFutureDecorator && !((AbstractRunnableFutureDecorator) task).isStarted()) {
      cancelledBeforeFireTasks.add(task);
    }
    tryTerminate();
  }

  protected void putTask(RunnableFuture<?> task, final ScheduledFuture<?> scheduledFuture) {
    scheduledTasks.put(task, scheduledFuture);
  }

  protected void removeTask(RunnableFuture<?> task) {
    scheduledTasks.remove(task);
  }

  private void tryTerminate() {
    if (isTerminated()) {
      terminationLatch.countDown();
    }
  }

  public ThreadType getThreadType() {
    return threadType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getThreadType() + " - " + getName() + "{" + lineSeparator()
        + "  executor: " + executor.toString() + lineSeparator()
        + "  shutdown: " + shutdown + lineSeparator()
        + "}";
  }
}
