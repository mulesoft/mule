/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Transaction aware {@link ExecutorService} decorator that does not scheduler tasks for async processing using the delegate
 * executor service if a transaction is active, instead a {@link MoreExecutors#newDirectExecutorService()} is used and the task is
 * run on the current thread.
 */
public class ConditionalExecutorServiceDecorator implements ScheduledExecutorService {

  private ScheduledExecutorService delegate;
  private Predicate<ScheduledExecutorService> scheduleOverridePredicate;
  private ExecutorService directExecutor = newDirectExecutorService();

  /**
   * Create a new executor service decorator that delegates to the provided executor service if no transaction is active and runs
   * tasks on the same thread otherwise.
   *
   * @param executorService the delegate executor service to use when no transaction is active.
   */
  public ConditionalExecutorServiceDecorator(ScheduledExecutorService executorService,
                                             Predicate<ScheduledExecutorService> scheduleOverridePredicate) {
    this.delegate = executorService;
    this.scheduleOverridePredicate = scheduleOverridePredicate;
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
    directExecutor.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    directExecutor.shutdownNow();
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return directExecutor.isShutdown() && delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return directExecutor.isTerminated() && delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return directExecutor.awaitTermination(0, unit) && delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.submit(task);
    } else {
      return delegate.submit(task);
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.submit(task, result);
    } else {
      return delegate.submit(task, result);
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.submit(task);
    } else {
      return delegate.submit(task);
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.invokeAll(tasks);
    } else {
      return delegate.invokeAll(tasks);
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.invokeAll(tasks, timeout, unit);
    } else {
      return delegate.invokeAll(tasks, timeout, unit);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.invokeAny(tasks);
    } else {
      return delegate.invokeAny(tasks);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (scheduleOverridePredicate.test(delegate)) {
      return directExecutor.invokeAny(tasks, timeout, unit);
    } else {
      return delegate.invokeAny(tasks, timeout, unit);
    }
  }

  @Override
  public void execute(Runnable command) {
    if (scheduleOverridePredicate.test(delegate)) {
      directExecutor.execute(command);
    } else {
      delegate.execute(command);
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    if (scheduleOverridePredicate.test(delegate)) {
      return synchronousSchedule(() -> {
        command.run();
        return null;
      }, delay, unit);
    } else {
      return delegate.schedule(command, delay, unit);
    }
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    if (scheduleOverridePredicate.test(delegate)) {
      return synchronousSchedule(callable, delay, unit);
    } else {
      return delegate.schedule(callable, delay, unit);
    }
  }

  private <V> ScheduledFuture<V> synchronousSchedule(Callable<V> callable, long delay, TimeUnit unit) {
    try {
      sleep(unit.toMillis(delay));
    } catch (InterruptedException e) {
      currentThread().interrupt();
      return new SynchronousScheduledFuture<>(true);
    }
    try {
      return new SynchronousScheduledFuture<>(callable.call());
    } catch (Exception e) {
      return new SynchronousScheduledFuture<>(e);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    if (scheduleOverridePredicate.test(delegate)) {
      throw new RejectedExecutionException("Cannot schedule recurrent tasks in a transactional context.");
    } else {
      return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    if (scheduleOverridePredicate.test(delegate)) {
      throw new RejectedExecutionException("Cannot schedule recurrent tasks in a transactional context.");
    } else {
      return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
  }

}
