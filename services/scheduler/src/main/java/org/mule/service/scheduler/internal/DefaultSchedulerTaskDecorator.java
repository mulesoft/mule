/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

/**
 * Decorates a {@link Runnable} in order to do hook behavior both before and after the execution of the decorated {@link Runnable}
 * so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
public class DefaultSchedulerTaskDecorator extends BaseSchedulerTaskDecorator implements Runnable {

  private final Runnable task;

  /**
   * Decorates the given {@code command}
   * 
   * @param task the task to be decorated
   * @param scheduler the owner {@link Executor} of this task
   */
  public DefaultSchedulerTaskDecorator(Runnable task, DefaultScheduler scheduler) {
    super(scheduler);
    this.task = task;
  }

  @Override
  public void run() {
    try {
      this.runner = currentThread();
      if (stopped) {
        return;
      }
      task.run();
    } finally {
      this.runner = null;
      wrapUp();
    }
  }

  /**
   * @return the task decorated by this decorator.
   */
  @Override
  public Runnable getDecoratedRunnable() {
    return task;
  }
}
