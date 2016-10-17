/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.nanoTime;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Proxy for a {@link ScheduledExecutorService} that adds tracking of the source of the dispatched tasks.
 *
 * @since 4.0
 */
public class DefaultScheduler implements Scheduler {

  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutor;

  private Set<BaseSchedulerTaskDecorator> currentTasks = synchronizedSet(new HashSet<>());

  private volatile boolean shutdown = false;

  /**
   * Wait condition to support awaitTermination
   */
  private final CountDownLatch terminationLatch = new CountDownLatch(1);

  /**
   * @param executor the actual executor that will run the dispatched tasks.
   * @param scheduledExecutor the executor that will handle the delayed/periodic tasks. This will not execute the actual tasks,
   *        but will dispatch it to the {@code executor} at the appropriate time.
   */
  public DefaultScheduler(ExecutorService executor, ScheduledExecutorService scheduledExecutor) {
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    checkShutdown();
    if (command == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(command);

    return scheduledExecutor.schedule(() -> executor.execute(decorated), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    if (callable == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = new DefaultSchedulerTaskDecorator(new FutureTask<>(callable), this);
    currentTasks.add(decorated);

    return (ScheduledFuture<V>) scheduledExecutor.schedule(() -> executor.execute(decorated), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    checkShutdown();
    if (command == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(command);

    return scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(decorated), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    if (command == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(command);

    return scheduledExecutor.scheduleWithFixedDelay(() -> executor.execute(decorated), initialDelay, delay, unit);
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
      synchronized (currentTasks) {
        tasks = new ArrayList<>(currentTasks.size());
        for (BaseSchedulerTaskDecorator task : currentTasks) {
          task.stop();
          tasks.add(task.getDecoratedRunnable());
        }
        currentTasks.clear();

        return tasks;
      }
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
    return shutdown && currentTasks.isEmpty();
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
  public <T> Future<T> submit(Callable<T> task) {
    checkShutdown();
    if (task == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerFutureTaskDecorator<T> decorated = decorateCallable(task);
    return executor.submit(decorated);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    checkShutdown();
    if (task == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(task);
    return executor.submit(decorated, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    checkShutdown();
    if (task == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(task);
    return executor.submit(decorated);
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();
    if (command == null) {
      throw new NullPointerException();
    }

    final DefaultSchedulerTaskDecorator decorated = decorateRunnable(command);
    executor.execute(decorated);
  }

  protected void checkShutdown() {
    if (isShutdown()) {
      throw new RejectedExecutionException(this.toString() + " already shutdown");
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    checkShutdown();
    if (tasks == null) {
      throw new NullPointerException();
    }

    final List<DefaultSchedulerFutureTaskDecorator<T>> decorated =
        tasks.stream().map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAll(decorated);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    checkShutdown();
    if (tasks == null) {
      throw new NullPointerException();
    }

    final List<DefaultSchedulerFutureTaskDecorator<T>> decorated =
        tasks.stream().map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAll(decorated, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    checkShutdown();
    if (tasks == null) {
      throw new NullPointerException();
    }

    final List<DefaultSchedulerFutureTaskDecorator<T>> decorated =
        tasks.stream().map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAny(decorated);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkShutdown();
    if (tasks == null) {
      throw new NullPointerException();
    }

    final List<DefaultSchedulerFutureTaskDecorator<T>> decorated =
        tasks.stream().map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAny(decorated, timeout, unit);
  }

  protected <T> DefaultSchedulerFutureTaskDecorator<T> decorateCallable(Callable<T> task) {
    final DefaultSchedulerFutureTaskDecorator<T> decorated = new DefaultSchedulerFutureTaskDecorator<>(task, this);
    currentTasks.add(decorated);
    return decorated;
  }

  protected DefaultSchedulerTaskDecorator decorateRunnable(Runnable task) {
    final DefaultSchedulerTaskDecorator decorated = new DefaultSchedulerTaskDecorator(task, this);
    currentTasks.add(decorated);
    return decorated;
  }

  protected void taskFinished(BaseSchedulerTaskDecorator task) {
    currentTasks.remove(task);
    tryTerminate();
  }

  private void tryTerminate() {
    if (isTerminated()) {
      terminationLatch.countDown();
    }
  }

  @Override
  public String toString() {
    return super.toString() + "{" + System.lineSeparator()
        + "  executor: " + executor.toString() + System.lineSeparator()
        + "  currentTasks: " + currentTasks.toString() + System.lineSeparator()
        + "  shutdown: " + shutdown + System.lineSeparator()
        + "}";
  }
}
