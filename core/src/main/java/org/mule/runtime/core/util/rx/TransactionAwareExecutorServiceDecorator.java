/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Transaction aware {@link ExecutorService} decorator that does not scheduler tasks for async processing using the delagate
 * executor service if a transaction is active, instead a {@link MoreExecutors#newDirectExecutorService()} is used and the task is
 * run on the current thread.
 */
public class TransactionAwareExecutorServiceDecorator implements ExecutorService {

  private ExecutorService delgate;
  private ExecutorService directExecutor = newDirectExecutorService();

  /**
   * Create a new executor service decorator that delegats to the provided executor service if no transaction is active and runs
   * tasks on the same thread otherwise.
   * 
   * @param executorService the delegate executor service to use when no transaction is actice.
   */
  public TransactionAwareExecutorServiceDecorator(ExecutorService executorService) {
    this.delgate = executorService;
  }

  @Override
  public void shutdown() {
    delgate.shutdown();
    directExecutor.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    directExecutor.shutdownNow();
    return delgate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return directExecutor.isShutdown() && delgate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return directExecutor.isTerminated() && delgate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return directExecutor.awaitTermination(timeout, unit) && delgate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    if (isTransactionActive()) {
      return directExecutor.submit(task);
    } else {
      return delgate.submit(task);
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    if (isTransactionActive()) {
      return directExecutor.submit(task, result);
    } else {
      return delgate.submit(task, result);
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    if (isTransactionActive()) {
      return directExecutor.submit(task);
    } else {
      return delgate.submit(task);
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    if (isTransactionActive()) {
      return directExecutor.invokeAll(tasks);
    } else {
      return delgate.invokeAll(tasks);
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    if (isTransactionActive()) {
      return directExecutor.invokeAll(tasks, timeout, unit);
    } else {
      return delgate.invokeAll(tasks, timeout, unit);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    if (isTransactionActive()) {
      return directExecutor.invokeAny(tasks);
    } else {
      return delgate.invokeAny(tasks);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (isTransactionActive()) {
      return directExecutor.invokeAny(tasks, timeout, unit);
    } else {
      return delgate.invokeAny(tasks, timeout, unit);
    }
  }

  @Override
  public void execute(Runnable command) {
    if (isTransactionActive()) {
      directExecutor.execute(command);
    } else {
      delgate.execute(command);
    }
  }

}
