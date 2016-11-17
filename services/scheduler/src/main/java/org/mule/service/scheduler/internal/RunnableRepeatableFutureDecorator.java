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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Decorates a {@link RunnableFuture} in order to do hook behavior before the execution of the decorated {@link RunnableFuture} so
 * a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
class RunnableRepeatableFutureDecorator<V> extends AbstractRunnableFutureDecorator<V> {

  private static final Logger logger = getLogger(RunnableRepeatableFutureDecorator.class);

  private final Supplier<RunnableFuture<V>> taskSupplier;
  private final Consumer<RunnableRepeatableFutureDecorator<V>> wrapUpCallback;

  private final DefaultScheduler scheduler;

  private volatile boolean running = false;
  private volatile boolean cancelled = false;
  private RunnableFuture<V> task;

  /**
   * Decorates the given {@code task}
   * 
   * @param task the task to be decorated
   * @param scheduler the owner {@link Executor} of this task
   */
  RunnableRepeatableFutureDecorator(Supplier<RunnableFuture<V>> taskSupplier,
                                    Consumer<RunnableRepeatableFutureDecorator<V>> wrapUpCallback,
                                    DefaultScheduler scheduler) {
    this.taskSupplier = taskSupplier;
    this.wrapUpCallback = wrapUpCallback;
    this.scheduler = scheduler;
  }

  @Override
  public void run() {
    if (running) {
      return;
    }
    if (cancelled) {
      logger.debug("Task " + this.toString() + " has been cancelled. Retunrning immendiately.");
      return;
    }

    long startTime = beforeRun();
    task = taskSupplier.get();
    try {
      running = true;
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
    super.wrapUp();
    wrapUpCallback.accept(this);
    running = false;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    this.cancelled = true;
    boolean success = true;
    if (task != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cancelling task " + this.toString() + " (mayInterruptIfRunning=" + mayInterruptIfRunning + ")...");
      }
      success = task.cancel(mayInterruptIfRunning);
    }
    scheduler.taskFinished(this);
    return success;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    if (task != null) {
      return task.isDone();
    } else {
      return false;
    }
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    if (task != null) {
      return task.get();
    } else {
      return null;
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (task != null) {
      return task.get(timeout, unit);
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "RunnableRepeatableFutureDecorator[" + taskSupplier.toString() + "]";
  }
}
