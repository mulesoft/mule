/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Decorates a {@link ExecutorService} to add logging. This is useful in cases like subscribeOn
 * that impose a Thread switch at the construction of the chain
 *
 * @since 4.2
 */
public class ThreadLoggingExecutorServiceDecorator implements ExecutorService {

  private ExecutorService delegate;
  private Optional<ThreadNotificationLogger> threadNotificationLogger;
  private CoreEvent event;

  public ThreadLoggingExecutorServiceDecorator(Optional<ThreadNotificationLogger> logger, ExecutorService delegate,
                                               CoreEvent event) {
    this.delegate = delegate;
    this.threadNotificationLogger = logger;
    this.event = event;
  }

  @Override
  public void shutdown() {
    this.delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    threadNotificationLogger.ifPresent(logger -> logger.setStartingThread(event));
    delegate.execute(command);
  }
}
