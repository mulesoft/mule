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
  private final ClassLoader classLoader;

  private final DefaultScheduler scheduler;

  private final String taskAsString;

  /**
   * Decorates the given {@code task}
   * 
   * @param task the task to be decorated
   * @param classLoader the context {@link ClassLoader} on which the {@code task} should be executed
   * @param scheduler the owner {@link Executor} of this task
   * @param taskAsString a {@link String} representation of the task, used for logging and troubleshooting.
   * @param id a unique it for this task.
   */
  RunnableFutureDecorator(RunnableFuture<V> task, ClassLoader classLoader, DefaultScheduler scheduler, String taskAsString,
                          Integer id) {
    super(id);
    this.task = task;
    this.classLoader = classLoader;
    this.scheduler = scheduler;
    this.taskAsString = taskAsString;
  }

  @Override
  public void run() {
    doRun(task, classLoader);
  }

  @Override
  protected void wrapUp() throws Exception {
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
    return taskAsString;
  }
}
