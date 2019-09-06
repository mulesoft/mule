/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Transaction aware {@link ExecutorService} decorator that does not scheduler tasks for async processing using the delegate
 * executor service if a transaction is active, instead a {@link MoreExecutors#newDirectExecutorService()} is used and the task is
 * run on the current thread.
 */
public class RejectionCallbackExecutorServiceDecorator implements ExecutorService {

  private final ExecutorService delegate;
  private final ScheduledExecutorService retryScheduler;
  private final Runnable onRejected;
  private final Runnable onRetrySuccessful;
  private final Duration retryInterval;

  /**
   * Create a new executor service decorator that delegates to the provided executor service if no transaction is active and runs
   * tasks on the same thread otherwise.
   *
   * @param executorService the delegate executor service to use when no transaction is active.
   * @param retryScheduler the executor service to use for scheduling the retries.
   * @param retryInterval
   */
  public RejectionCallbackExecutorServiceDecorator(ExecutorService executorService, ScheduledExecutorService retryScheduler,
                                                   Runnable onRejected, Runnable onRetrySuccessful, Duration retryInterval) {
    this.delegate = executorService;
    this.retryScheduler = retryScheduler;
    this.onRejected = onRejected;
    this.onRetrySuccessful = onRetrySuccessful;
    this.retryInterval = retryInterval;
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    try {
      return delegate.submit(task);
    } catch (RejectedExecutionException ree) {
      onRejected.run();

      CompletableFuture<T> retryedFuture = new CompletableFuture<>();
      retryScheduler.schedule(() -> {
        submit(() -> {
          try {
            final T call = task.call();
            retryedFuture.complete(call);
            return call;
          } catch (Exception e) {
            retryedFuture.completeExceptionally(e);
            throw e;
          }
        });
      }, retryInterval.toMillis(), MILLISECONDS);
      return retryedFuture.thenApply(r -> {
        onRetrySuccessful.run();
        return r;
      });
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    try {
      return delegate.submit(task, result);
    } catch (RejectedExecutionException e) {
      onRejected.run();

      CompletableFuture<T> retryedFuture = new CompletableFuture<>();
      retryScheduler.schedule(() -> {
        submit(() -> {
          task.run();
          retryedFuture.complete(null);
        }, result);
      }, retryInterval.toMillis(), MILLISECONDS);
      return retryedFuture.thenApply(r -> {
        onRetrySuccessful.run();
        return r;
      });
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    try {
      return delegate.submit(task);
    } catch (RejectedExecutionException e) {
      onRejected.run();

      CompletableFuture<?> retryedFuture = new CompletableFuture<>();
      retryScheduler.schedule(() -> {
        submit(() -> {
          task.run();
          retryedFuture.complete(null);
        });
      }, retryInterval.toMillis(), MILLISECONDS);
      return retryedFuture.thenApply(r -> {
        onRetrySuccessful.run();
        return r;
      });
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    try {
      delegate.execute(command);
    } catch (RejectedExecutionException e) {
      onRejected.run();
    }
  }
  //
  // @Override
  // public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
  // return delegate.schedule(command, delay, unit);
  // }
  //
  // @Override
  // public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
  // return delegate.schedule(callable, delay, unit);
  // }
  //
  // // private <V> ScheduledFuture<V> synchronousSchedule(Callable<V> callable, long delay, TimeUnit unit) {
  // // try {
  // // sleep(unit.toMillis(delay));
  // // } catch (InterruptedException e) {
  // // currentThread().interrupt();
  // // return new SynchronousScheduledFuture<>(true);
  // // }
  // // try {
  // // return new SynchronousScheduledFuture<>(callable.call());
  // // } catch (Exception e) {
  // // return new SynchronousScheduledFuture<>(e);
  // // }
  // // }
  //
  // @Override
  // public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
  // return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
  // }
  //
  // @Override
  // public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
  // return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  // }

}
