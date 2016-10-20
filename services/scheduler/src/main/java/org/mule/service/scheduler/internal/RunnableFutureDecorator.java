/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Decorates a {@link RunnableFuture} in order to do hook behavior both before and after the execution of the decorated
 * {@link RunnableFuture} so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
class RunnableFutureDecorator<V> implements RunnableFuture<V> {

  private final RunnableFuture<V> task;

  private final DefaultScheduler scheduler;

  private volatile boolean started = false;
  private volatile boolean stopped = false;

  /**
   * Decorates the given {@code task}
   * 
   * @param task the task to be decorated
   * @param scheduler the owner {@link Executor} of this task
   */
  RunnableFutureDecorator(RunnableFuture<V> task, DefaultScheduler scheduler) {
    this.task = task;
    this.scheduler = scheduler;
  }

  @Override
  public void run() {
    try {
      if (!startTask()) {
        // In case the scheduler that owns this task was shutdown, we behave consistently by just not executing the task.
        return;
      }
      task.run();
    } finally {
      wrapUp();
    }
  }

  private void wrapUp() {
    scheduler.taskFinished(this);
  }

  private boolean startTask() {
    if (!stopped) {
      this.started = true;
    }
    return !stopped;
  }

  /**
   * @return {@code true} if the execution of this task has already started, false otherwise.
   */
  boolean isStarted() {
    return started;
  }

  /**
   * Marks this task as stopped so is is not executed when started, and interrupts its thread if it has been already started.
   */
  protected void doCancel() {
    this.stopped = true;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    doCancel();
    return task.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return task.isCancelled();
  }

  @Override
  public boolean isDone() {
    return task.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return task.get();
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return task.get(timeout, unit);
  }
}
