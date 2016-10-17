/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * Decorates a {@link RunnableFuture} in order to do hook behavior both before and after the execution of the decorated
 * {@link RunnableFuture} so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
public class DefaultSchedulerFutureTaskDecorator<V> extends BaseSchedulerTaskDecorator implements Callable<V> {

  private final Callable<V> task;

  /**
   * Decorates the given {@code command}
   * 
   * @param task the task to be decorated
   * @param scheduler the owner {@link Executor} of this task
   */
  public DefaultSchedulerFutureTaskDecorator(Callable<V> task, DefaultScheduler scheduler) {
    super(scheduler);
    this.task = task;
  }

  @Override
  public V call() throws Exception {
    try {
      this.runner = Thread.currentThread();
      if (stopped) {
        return null;
      }
      return task.call();
    } finally {
      this.runner = null;
      wrapUp();
    }
  }

  @Override
  public Runnable getDecoratedRunnable() {
    return new FutureTask<>(task);
  }
}
