/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.util.rx.SynchronousScheduledFuture.synchronousSchedule;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Scheduler} implementation that doesn't do any threading.
 *
 * @since 4.2
 */
public class ImmediateScheduler implements Scheduler {

  public static final Scheduler IMMEDIATE_SCHEDULER = new ImmediateScheduler();

  private ImmediateScheduler() {
    // Nothing to do
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return synchronousSchedule(() -> {
      command.run();
      return null;
    }, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return synchronousSchedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new RejectedExecutionException("Cannot schedule recurrent tasks in an ImmediateScheduler.");
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new RejectedExecutionException("Cannot schedule recurrent tasks in an ImmediateScheduler.");
  }

  @Override
  public void shutdown() {
    // Nothing to do
  }

  @Override
  public List<Runnable> shutdownNow() {
    return emptyList();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return false;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    try {
      return immediateFuture(task.call());
    } catch (Exception e) {
      return immediateFailedFuture(e);
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    try {
      task.run();
      return immediateFuture(result);
    } catch (Exception e) {
      return immediateFailedFuture(e);
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    try {
      task.run();
      return immediateFuture(null);
    } catch (Exception e) {
      return immediateFailedFuture(e);
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return tasks.stream().map(t -> submit(t)).collect(toList());
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return tasks.stream().map(t -> submit(t)).collect(toList());
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    try {
      return tasks.iterator().next().call();
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    try {
      return tasks.iterator().next().call();
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    throw new RejectedExecutionException("Cannot schedule recurrent tasks in an ImmediateScheduler.");
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    throw new RejectedExecutionException("Cannot schedule recurrent tasks in an ImmediateScheduler.");
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  @Override
  public String getName() {
    return "Immediate Scheduler";
  }

}
