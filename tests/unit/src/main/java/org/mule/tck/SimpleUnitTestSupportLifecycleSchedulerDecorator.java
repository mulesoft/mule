/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleUnitTestSupportLifecycleSchedulerDecorator implements Scheduler {

  private Scheduler decorated;
  private boolean stopped;

  public SimpleUnitTestSupportLifecycleSchedulerDecorator(Scheduler decorated) {
    super();
    this.decorated = decorated;
  }

  @Override
  public void stop(long gracefulShutdownTimeout, TimeUnit unit) {
    this.stopped = true;
    decorated.stop(gracefulShutdownTimeout, unit);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return decorated.schedule(command, delay, unit);
  }

  @Override
  public void execute(Runnable command) {
    decorated.execute(command);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return decorated.schedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return decorated.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return decorated.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    return decorated.scheduleWithCronExpression(command, cronExpression);
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    return decorated.scheduleWithCronExpression(command, cronExpression, timeZone);
  }

  @Override
  public void shutdown() {
    this.stopped = true;
    decorated.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    this.stopped = true;
    return decorated.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return this.stopped || decorated.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return decorated.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return decorated.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return decorated.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return decorated.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return decorated.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return decorated.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return decorated.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return decorated.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return decorated.invokeAny(tasks, timeout, unit);
  }

}
