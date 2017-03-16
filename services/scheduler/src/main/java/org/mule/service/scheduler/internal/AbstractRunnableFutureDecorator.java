/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.slf4j.Logger;

/**
 * Abstract base decorator for a a {@link RunnableFuture} in order to do hook behavior before the execution of the decorated
 * {@link RunnableFuture} so a consistent state is maintained in the owner {@link DefaultScheduler}.
 *
 * @since 4.0
 */
abstract class AbstractRunnableFutureDecorator<V> implements RunnableFuture<V> {

  private static final Logger logger = getLogger(AbstractRunnableFutureDecorator.class);

  private static Field threadLocalsField;

  static {
    try {
      threadLocalsField = Thread.class.getDeclaredField("threadLocals");
      threadLocalsField.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void clearAllThreadLocals() {
    try {
      threadLocalsField.set(currentThread(), null);
    } catch (Exception e) {
      new MuleRuntimeException(e);
    }
  }

  private final Integer id;

  private volatile boolean started = false;

  /**
   * @param id a unique it for this task.
   */
  protected AbstractRunnableFutureDecorator(Integer id) {
    this.id = id;
  }

  protected long beforeRun() {
    long startTime = 0;
    if (logger.isTraceEnabled()) {
      startTime = System.nanoTime();
      logger.trace("Starting task " + this.toString() + "...");
    }
    started = true;
    return startTime;
  }

  /**
   * Performas the required bookkeeping before and after running the task, as well as setting the appropriate context for the
   * task.
   * <p>
   * Any {@link Exception} thrown as part of the task processing or bookkeeping is handled by this method and not rethrown.
   * 
   * @param task the task to run
   * @param classLoader the classloader to put in the context of the task when run
   */
  protected void doRun(RunnableFuture<V> task, ClassLoader classLoader) {
    long startTime = beforeRun();

    final Thread currentThread = currentThread();
    final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(classLoader);

    try {
      task.run();
      task.get();
    } catch (ExecutionException e) {
      logger.error("Uncaught throwable in task " + toString(), e);
    } catch (CancellationException e) {
      // Log instead of rethrow to avoid flooding the logger with stack traces of cancellation, which may be very common.
      logger.trace("Task " + toString() + " cancelled");
    } catch (InterruptedException e) {
      currentThread.interrupt();
    } finally {
      try {
        wrapUp();
      } catch (Exception e) {
        logger.error("Exception wrapping up execution of " + task.toString(), e);
      } finally {
        if (logger.isTraceEnabled()) {
          logger.trace("Task " + this.toString() + " finished after " + (nanoTime() - startTime) + " nanoseconds");
        }

        currentThread.setContextClassLoader(currentClassLoader);
      }
    }
  }

  protected void wrapUp() throws Exception {
    started = false;
    clearAllThreadLocals();
  }

  /**
   * @return {@code true} if the execution of this task has already started, false otherwise.
   */
  boolean isStarted() {
    return started;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
