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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  /**
   * Wait condition to support awaitTermination
   */
  private final CountDownLatch terminationLatch = new CountDownLatch(1);

  private static final ScheduledFuture<?> NULL_SCHEDULED_FUTURE = NullScheduledFuture.INSTANCE;
  private Map<RunnableFuture<?>, ScheduledFuture<?>> scheduledTasks;

  private volatile boolean shutdown = false;


  /**
   * @param executor the actual executor that will run the dispatched tasks.
   * @param workers
   * @param cores
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   */
  DefaultScheduler(ExecutorService executor, int workers, int totalWorkers, ScheduledExecutorService scheduledExecutor) {
    scheduledTasks = new ConcurrentHashMap<>(workers, 1.00f, totalWorkers);
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFutureDecorator<?> task = new RunnableFutureDecorator<>(super.newTaskFor(command, null), this);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(callable);

    final RunnableFutureDecorator<V> task = new RunnableFutureDecorator<>(super.newTaskFor(callable), this);

    final ScheduledFutureDecorator<V> scheduled =
        new ScheduledFutureDecorator(scheduledExecutor.schedule(schedulableTask(task), delay, unit), task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFutureDecorator<?> task = new RunnableFutureDecorator<>(super.newTaskFor(command, null), this);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.scheduleAtFixedRate(schedulableTask(task), initialDelay, period, unit),
                                       task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final RunnableFutureDecorator<?> task = new RunnableFutureDecorator<>(super.newTaskFor(command, null), this);

    final ScheduledFutureDecorator<?> scheduled =
        new ScheduledFutureDecorator<>(scheduledExecutor.scheduleWithFixedDelay(schedulableTask(task), initialDelay, delay, unit),
                                       task);

    scheduledTasks.put(task, scheduled);
    return scheduled;
  }

  private Runnable schedulableTask(RunnableFuture<?> task) {
    return () -> executor.execute(task);
  }

  @Override
  public void shutdown() {
    this.shutdown = true;
    tryTerminate();
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();

    List<Runnable> tasks;
    try {
      tasks = new ArrayList<>(scheduledTasks.size());

      for (Entry<RunnableFuture<?>, ScheduledFuture<?>> taskEntry : scheduledTasks.entrySet()) {
        taskEntry.getValue().cancel(true);
        taskEntry.getKey().cancel(true);
        if (taskEntry.getKey() instanceof RunnableFutureDecorator
            && !((RunnableFutureDecorator<?>) taskEntry.getKey()).isStarted()) {
          tasks.add(taskEntry.getKey());
        }
      }
      scheduledTasks.clear();

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
    final RunnableFutureDecorator<T> decorated = new RunnableFutureDecorator<>(super.newTaskFor(callable), this);
    scheduledTasks.put(decorated, NULL_SCHEDULED_FUTURE);
    return decorated;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    final RunnableFutureDecorator<T> decorated = new RunnableFutureDecorator<>(super.newTaskFor(runnable, value), this);
    scheduledTasks.put(decorated, NULL_SCHEDULED_FUTURE);
    return decorated;
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();
    requireNonNull(command);

    executor.execute(command);
  }

  protected void checkShutdown() {
    if (isShutdown()) {
      throw new RejectedExecutionException(this.toString() + " already shutdown");
    }
  }

  protected void taskFinished(RunnableFutureDecorator<?> task) {
    scheduledTasks.remove(task);
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
