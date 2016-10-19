/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Base decorator for tasks to be executed in a {@link DefaultScheduler}, in order to do hook behavior both before and after the
 * execution of the decorated task.
 *
 * @since 4.0
 */
public abstract class BaseSchedulerTaskDecorator {

  private final DefaultScheduler scheduler;

  private volatile Future<?> submittedFuture;

  private volatile Thread runner;

  private volatile boolean stopped = false;

  /**
   * @param scheduler the owner {@link Executor} of this task
   */
  protected BaseSchedulerTaskDecorator(DefaultScheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Sets the {@link Future} associated to this task decorator, so it may be cancelled when {@link #stop()} is called.
   * <p>
   * If {@link #stop()} has already been called, {@code submittedFuture} will be immediately cancelled.
   * 
   * @param submittedFuture
   */
  public void linkWithSubmittedFuture(Future<?> submittedFuture) {
    if (stopped) {
      submittedFuture.cancel(true);
    } else {
      this.submittedFuture = submittedFuture;
    }
  }

  protected void wrapUp() {
    this.runner = null;
    scheduler.taskFinished(this);
  }

  protected boolean start() {
    if (!stopped) {
      this.runner = currentThread();
    }
    return !stopped;
  }

  /**
   * @return
   */
  public boolean isStarted() {
    return runner != null;
  }

  /**
   * Marks this task as stopped so is is not executed when started, and interrupts its thread if it has been already started.
   */
  public void stop() {
    this.stopped = true;
    if (submittedFuture != null) {
      submittedFuture.cancel(true);
    }
    if (runner != null) {
      runner.interrupt();
    }
  }

  /**
   * @return the {@link Runnable} command that this decorator is decorating.
   */
  public abstract Runnable getDecoratedRunnable();
}
