/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import org.mule.runtime.api.scheduler.Scheduler;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Transaction aware {@link ExecutorService} decorator that does not scheduler tasks for async processing using the delagate
 * executor service if a transaction is active, instead a {@link MoreExecutors#newDirectExecutorService()} is used and the task is
 * run on the current thread.
 */
public class ConditionalExecutorServiceDecorator implements ExecutorService {

  private Scheduler delegate;
  private Predicate<Scheduler> scheduleOverridePredicate;
  private ExecutorService directExecutor = newDirectExecutorService();

  /**
   * Create a new executor service decorator that delegats to the provided executor service if no transaction is active and runs
   * tasks on the same thread otherwise.
   * 
   * @param executorService the delegate executor service to use when no transaction is actice.
   */
  public ConditionalExecutorServiceDecorator(Scheduler executorService, Predicate<Scheduler> scheduleOverridePredicate) {
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

}
