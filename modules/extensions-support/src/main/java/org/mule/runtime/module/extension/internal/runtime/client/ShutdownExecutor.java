/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ForwardingExecutorService;

public final class ShutdownExecutor extends ForwardingExecutorService {

  private final AtomicInteger tasks = new AtomicInteger();
  private final Semaphore semaphore = new Semaphore(0);
  private volatile boolean shutdown;

  @Override
  public void execute(Runnable task) {
    if (shutdown) {
      throw new RejectedExecutionException("Shutdown");
    }

    tasks.incrementAndGet();
    delegate().execute(() -> {
      try {
        task.run();
      } finally {
        semaphore.release();
      }
    });
  }

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    int permits = tasks.get();
    boolean terminated = semaphore.tryAcquire(permits, timeout, unit);
    if (terminated) {
      semaphore.release(permits);
    }
    return terminated && shutdown;
  }

  @Override
  public boolean isTerminated() {
    try {
      return awaitTermination(0, SECONDS);
    } catch (InterruptedException e) {
      return false;
    }
  }

  @Override
  protected ExecutorService delegate() {
    return commonPool();
  }
}
