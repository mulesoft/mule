/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.Reference;

import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper Scheduler that retries to submit task if {@link RejectedExecutionException} is thrown, applying
 * a callback each time it needs to retry.
 * This is needed to avoid terminating a Reactor Flux after a {@link RejectedExecutionException} since
 * `publishOn` does not support `onErrorContinue`: https://github.com/reactor/reactor-core/issues/1488
 *
 * @since 4.2
 */
public class RetrySchedulerWrapper implements Scheduler {

  private Scheduler delegate;
  private long retryTime;
  private Runnable onRetry;

  public RetrySchedulerWrapper(Scheduler delegate, long retryTime, Runnable onRetry) {
    this.delegate = delegate;
    this.retryTime = retryTime;
    this.onRetry = onRetry;
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    return delegate.scheduleWithCronExpression(command, cronExpression);
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    return delegate.scheduleWithCronExpression(command, cronExpression, timeZone);
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

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

  private void runWithRetry(Runnable operation) {
    while (!this.isShutdown() && !this.isTerminated()) {
      try {
        operation.run();
        return;
      } catch (RejectedExecutionException ree) {
        onRetry.run();
        try {
          Thread.sleep(retryTime);
        } catch (InterruptedException e) {
          throw new RejectedExecutionException();
        }
      }
    }
    throw new RejectedExecutionException();
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    Reference<Future<T>> futureReference = new Reference<>();
    runWithRetry(() -> futureReference.set(delegate.submit(task)));
    return futureReference.get();
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    Reference<Future<T>> futureReference = new Reference<>();
    runWithRetry(() -> futureReference.set(delegate.submit(task, result)));
    return futureReference.get();
  }

  @Override
  public Future<?> submit(Runnable task) {
    Reference<Future<?>> futureReference = new Reference<>();
    runWithRetry(() -> futureReference.set(delegate.submit(task)));
    return futureReference.get();
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
    runWithRetry(() -> delegate.execute(command));
  }
}
