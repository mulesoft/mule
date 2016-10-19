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
 * Decorates a {@link Runnable} in order to do hook behavior both before and after the execution of the decorated {@link Runnable}
 * so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
public class SchedulerRunnableDecorator<V> extends BaseSchedulerTaskDecorator implements RunnableFuture<V> {

  private final RunnableFuture<V> task;

  /**
   * Decorates the given {@code task}
   * 
   * @param task the task to be decorated
   * @param scheduler the owner {@link Executor} of this task
   */
  public SchedulerRunnableDecorator(RunnableFuture<V> task, DefaultScheduler scheduler) {
    super(scheduler);
    this.task = task;
  }

  @Override
  public void run() {
    try {
      if (!start()) {
        return;
      }
      task.run();
    } finally {
      wrapUp();
    }
  }

  @Override
  protected void doCancelTask() {
    task.cancel(true);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
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
