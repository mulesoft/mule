/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.lineSeparator;
import static java.lang.System.nanoTime;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
    requireNonNull(command);

    final SchedulerRunnableDecorator decorated = decorateRunnable(command);

    final ScheduledFuture<?> submittedFuture = scheduledExecutor.schedule(() -> executor.execute(decorated), delay, unit);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(callable);

    final SchedulerRunnableDecorator decorated = new SchedulerRunnableDecorator(new FutureTask<>(callable), this);
    currentTasks.add(decorated);
    final ScheduledFuture<V> submittedFuture =
        (ScheduledFuture<V>) scheduledExecutor.schedule(() -> executor.execute(decorated), delay, unit);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final SchedulerRunnableDecorator decorated = decorateRunnable(command);

    final ScheduledFuture<?> submittedFuture =
        scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(decorated), initialDelay, period, unit);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    checkShutdown();
    requireNonNull(command);

    final SchedulerRunnableDecorator decorated = decorateRunnable(command);

    final ScheduledFuture<?> submittedFuture =
        scheduledExecutor.scheduleWithFixedDelay(() -> executor.execute(decorated), initialDelay, delay, unit);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
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
          if (!task.isStarted()) {
            tasks.add(task.getDecoratedRunnable());
          }
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
    requireNonNull(task);

    final SchedulerCallableDecorator<T> decorated = decorateCallable(task);
    final Future<T> submittedFuture = executor.submit(decorated);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    checkShutdown();
    requireNonNull(task);

    final SchedulerRunnableDecorator decorated = decorateRunnable(task);
    final Future<T> submittedFuture = executor.submit(decorated, result);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public Future<?> submit(Runnable task) {
    checkShutdown();
    requireNonNull(task);

    final SchedulerRunnableDecorator decorated = decorateRunnable(task);
    final Future<?> submittedFuture = executor.submit(decorated);
    decorated.linkWithSubmittedFuture(submittedFuture);
    return submittedFuture;
  }

  @Override
  public void execute(Runnable command) {
    checkShutdown();
    requireNonNull(command);

    final SchedulerRunnableDecorator decorated = decorateRunnable(command);
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
    requireNonNull(tasks);

    final List<SchedulerCallableDecorator<T>> decoratedTasks =
        tasks.stream().map(t -> requireNonNull(t)).map(t -> decorateCallable(t)).collect(toList());
    final List<Future<T>> submittedFutures = executor.invokeAll(decoratedTasks);

    linkTasksWithSubmittedFutures(decoratedTasks, submittedFutures);
    return submittedFutures;
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    checkShutdown();
    requireNonNull(tasks);

    final List<SchedulerCallableDecorator<T>> decoratedTasks =
        tasks.stream().map(t -> requireNonNull(t)).map(t -> decorateCallable(t)).collect(toList());
    final List<Future<T>> submittedFutures = executor.invokeAll(decoratedTasks, timeout, unit);

    linkTasksWithSubmittedFutures(decoratedTasks, submittedFutures);
    return submittedFutures;
  }

  protected <T> void linkTasksWithSubmittedFutures(final List<SchedulerCallableDecorator<T>> decoratedTasks,
                                                   final List<Future<T>> submittedFutures) {
    // This will work because we converted the parameter collection to a list, so the consistent order is ensured.
    Iterator<SchedulerCallableDecorator<T>> it1 = decoratedTasks.iterator();
    Iterator<Future<T>> it2 = submittedFutures.iterator();
    while (it1.hasNext() && it2.hasNext()) {
      SchedulerCallableDecorator<T> decorated = it1.next();
      Future<T> submittedFuture = it2.next();
      decorated.linkWithSubmittedFuture(submittedFuture);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    checkShutdown();
    requireNonNull(tasks);

    final List<SchedulerCallableDecorator<T>> decoratedTasks =
        tasks.stream().map(t -> requireNonNull(t)).map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAny(decoratedTasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkShutdown();
    requireNonNull(tasks);

    final List<SchedulerCallableDecorator<T>> decorated =
        tasks.stream().map(t -> requireNonNull(t)).map(t -> decorateCallable(t)).collect(toList());
    return executor.invokeAny(decorated, timeout, unit);
  }

  protected <T> SchedulerCallableDecorator<T> decorateCallable(Callable<T> task) {
    final SchedulerCallableDecorator<T> decorated = new SchedulerCallableDecorator<>(task, this);
    currentTasks.add(decorated);
    return decorated;
  }

  protected SchedulerRunnableDecorator decorateRunnable(Runnable task) {
    final SchedulerRunnableDecorator decorated = new SchedulerRunnableDecorator(task, this);
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
    return super.toString() + "{" + lineSeparator()
        + "  executor: " + executor.toString() + lineSeparator()
        + "  currentTasks: " + currentTasks.toString() + lineSeparator()
        + "  shutdown: " + shutdown + lineSeparator()
        + "}";
  }
}
