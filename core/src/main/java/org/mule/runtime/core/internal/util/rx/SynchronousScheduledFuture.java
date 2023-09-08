/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.core.internal.processor.interceptor.InterceptionException;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class SynchronousScheduledFuture<V> implements ScheduledFuture<V> {

  static <V> ScheduledFuture<V> synchronousSchedule(Callable<V> callable, long delay, TimeUnit unit) {
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

  private V result;
  private Throwable thrown;
  private boolean interrupted;

  public SynchronousScheduledFuture(V result) {
    this.result = result;
  }

  public SynchronousScheduledFuture(Throwable thrown) {
    this.thrown = thrown;
  }

  public SynchronousScheduledFuture(boolean interrupted) {
    this.interrupted = interrupted;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return 0;
  }

  @Override
  public int compareTo(Delayed o) {
    return o.getDelay(MILLISECONDS) == 0L ? 0 : -1;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    if (interrupted) {
      throw new InterceptionException();
    } else if (result != null) {
      return result;
    } else {
      throw new ExecutionException(thrown);
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }
}
