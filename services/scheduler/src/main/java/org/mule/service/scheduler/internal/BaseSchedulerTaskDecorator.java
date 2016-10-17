/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import java.util.concurrent.Executor;

/**
 * Base decorator for tasks to be executed in a {@link DefaultScheduler}, in order to do hook behavior both before and after the
 * execution of the decorated task.
 *
 * @since 4.0
 */
public abstract class BaseSchedulerTaskDecorator {

  private final DefaultScheduler scheduler;

  protected volatile Thread runner;

  protected volatile boolean stopped = false;

  /**
   * @param scheduler the owner {@link Executor} of this task
   */
  protected BaseSchedulerTaskDecorator(DefaultScheduler scheduler) {
    this.scheduler = scheduler;
  }

  protected void wrapUp() {
    scheduler.taskFinished(this);
  }

  /**
   * Marks this task as stopped so is is not executed when started, and interrupts its thread if it has been already started.
   */
  public void stop() {
    this.stopped = true;
    if (runner != null) {
      runner.interrupt();
    }
  }

  /**
   * @return the {@link Runnable} command that this decorator is decorating.
   */
  public abstract Runnable getDecoratedRunnable();
}
