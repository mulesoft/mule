/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.lang.reflect.Field;
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
class RunnableFutureDecorator<V> implements RunnableFuture<V> {

  private static final Logger logger = getLogger(RunnableFutureDecorator.class);

  private static Field threadLocalsField;

  static {
    try {
      threadLocalsField = Thread.class.getDeclaredField("threadLocals");
      threadLocalsField.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private final RunnableFuture<V> task;

  private final DefaultScheduler scheduler;

  private volatile boolean started = false;

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
    long startTime = 0;
    if (logger.isDebugEnabled()) {
      startTime = System.nanoTime();
      logger.debug("Starting task " + this.toString() + "...");
    }
    started = true;
    try {
      task.run();
    } finally {
      wrapUp();
      if (logger.isDebugEnabled()) {
        logger.debug("Task " + this.toString() + " finished after " + (System.nanoTime() - startTime) + " nanoseconds");
      }
    }
  }

  private void wrapUp() {
    scheduler.taskFinished(this);
    clearAllThreadLocals();
  }

  public static void clearAllThreadLocals() {
    try {
      threadLocalsField.set(currentThread(), null);
    } catch (Exception e) {
      new MuleRuntimeException(e);
    }
  }

  /**
   * @return {@code true} if the execution of this task has already started, false otherwise.
   */
  boolean isStarted() {
    return started;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (logger.isDebugEnabled()) {
      logger.debug("Cancelling task " + this.toString() + " (mayInterruptIfRunning=" + mayInterruptIfRunning + ")...");
    }
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

  @Override
  public String toString() {
    return "RunnableFutureDecorator[" + task.toString() + "]";
  }
}
