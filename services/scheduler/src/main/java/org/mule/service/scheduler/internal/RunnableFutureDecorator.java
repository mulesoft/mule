/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

/**
 * Decorates a {@link RunnableFuture} in order to do hook behavior both before and after the execution of the decorated
 * {@link RunnableFuture} so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
class RunnableFutureDecorator<V> extends AbstractRunnableFutureDecorator<V> {

  private static final Logger logger = getLogger(RunnableFutureDecorator.class);

  private final RunnableFuture<V> task;

  private final DefaultScheduler scheduler;

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
    long startTime = beforeRun();
    try {
      task.run();
    } finally {
      wrapUp();
      if (logger.isTraceEnabled()) {
        logger.trace("Task " + this.toString() + " finished after " + (System.nanoTime() - startTime) + " nanoseconds");
      }
    }
  }

  @Override
  protected void wrapUp() {
    scheduler.taskFinished(this);
    super.wrapUp();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (logger.isDebugEnabled()) {
      logger.debug("Cancelling task " + this.toString() + " (mayInterruptIfRunning=" + mayInterruptIfRunning + ")...");
    }
    boolean success = task.cancel(mayInterruptIfRunning);
    scheduler.taskFinished(this);
    return success;
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

  @Override
  public String toString() {
    return "RunnableFutureDecorator[" + task.toString() + "]";
  }
}
