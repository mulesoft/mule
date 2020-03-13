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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.RuntimeErrorException;

/**
 * Decorator for {@link ScheduledExecutorService} that will schedule many retries when a {@link RejectedExecutionException} is
 * thrown.
 * <p>
 * Retries will be done every time a {@link RuntimeErrorException} is thrown. If for one task, {@link RejectedExecutionException}
 * is consistently thrown, retries will be scheduled indefinitely until the task is finally accepted.
 *
 * @since 4.3, 4.2.3
 */
public class RejectionCallbackExecutorServiceDecorator implements ScheduledExecutorService {

  private final ScheduledExecutorService delegate;
  private final ScheduledExecutorService retryScheduler;
  private final Runnable onRejected;
  private final Runnable onRetrySuccessful;
  private final Duration retryInterval;

  /**
   *
   * @param executorService the executors service to decorate
   * @param retryScheduler the executor to use for scheduling the retries
   * @param onRejected callback to be executed when a {@link RejectedExecutionException} is thrown by {@code executorService},
   *        before scheduling the retry.
   * @param onRetrySuccessful callback to be executed after a retry has been successful.
   * @param retryInterval the delay for the scheduled retries.
   */
  public RejectionCallbackExecutorServiceDecorator(ScheduledExecutorService executorService,
                                                   ScheduledExecutorService retryScheduler,
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
        if (task instanceof RetriedCallable) {
          submit(task);
        } else {
          submit((RetriedCallable<T>) () -> {
            try {
              final T call = task.call();
              retryedFuture.complete(call);
              return call;
            } catch (Exception e) {
              retryedFuture.completeExceptionally(e);
              throw e;
            }
          });
        }

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
        if (task instanceof RetriedRunnable) {
          submit(task);
        } else {
          submit((RetriedRunnable) () -> {
            task.run();
            retryedFuture.complete(null);
          }, result);
        }
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
        if (task instanceof RetriedRunnable) {
          submit(task);
        } else {
          submit((RetriedRunnable) () -> {
            task.run();
            retryedFuture.complete(null);
          });
        }
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

  /////////
  // For the schedule methods, nothing to do in particular.
  /////////

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(command, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return delegate.schedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  private interface RetriedCallable<T> extends Callable<T> {

  }

  private interface RetriedRunnable extends Runnable {

  }

}
