/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.time.Duration;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

public class RejectionCallbackExecutorServiceDecoratorTestCase extends AbstractMuleTestCase {

  private static final int RECEIVE_TIMEOUT = 5000;
  private static final int REJECTION_COUNT = 10000;

  private ScheduledExecutorService retryScheduler;
  private RejectionCallbackExecutorServiceDecorator decorated;

  @Before
  public void before() {
    retryScheduler = newSingleThreadScheduledExecutor();
    decorated = new RejectionCallbackExecutorServiceDecorator(new RejectingScheduler(retryScheduler),
                                                              retryScheduler,
                                                              () -> {
                                                              },
                                                              () -> {
                                                              },
                                                              Duration.of(1, NANOS));
  }

  @After
  public void after() {
    decorated.shutdownNow();
    retryScheduler.shutdownNow();
  }

  @Test
  @Issue("MULE-18179")
  public void tooManyRejectionsSubmit() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    decorated.submit(() -> {
      latch.countDown();
      return 0;
    });

    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  @Issue("MULE-18179")
  public void tooManyRejectionsSubmitRunnableResult() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    decorated.submit(() -> {
      latch.countDown();
    }, -1);

    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  @Issue("MULE-18179")
  public void tooManyRejectionsSubmitRunnable() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    decorated.submit(() -> {
      latch.countDown();
    });

    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  static class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    private final String threadNamePrefix;
    private final ExecutorService executor;

    public TestScheduler(int threads, String threadNamePrefix, boolean reject) {
      super(1, new NamedThreadFactory(threadNamePrefix + ".tasks"));
      this.threadNamePrefix = threadNamePrefix;
      executor = new ThreadPoolExecutor(threads, threads, 0l, TimeUnit.MILLISECONDS,
                                        new LinkedBlockingQueue(reject ? threads : Integer.MAX_VALUE),
                                        new NamedThreadFactory(threadNamePrefix));
    }

    @Override
    public Future<?> submit(Runnable task) {
      return executor.submit(task);
    }

    @Override
    public Future<?> submit(Callable task) {
      return executor.submit(task);
    }

    @Override
    public void stop() {
      shutdown();
      executor.shutdown();
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
      throw new UnsupportedOperationException(
                                              "Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
      throw new UnsupportedOperationException(
                                              "Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
    }

    @Override
    public String getName() {
      return threadNamePrefix;
    }

  }

  /**
   * Scheduler that rejects tasks {@link #REJECTION_COUNT} times and then delegates to delegate scheduler.
   */
  static class RejectingScheduler extends TestScheduler {

    private final AtomicInteger rejections = new AtomicInteger();
    private final AtomicInteger accepted = new AtomicInteger();
    private final ExecutorService delegate;

    public RejectingScheduler(ExecutorService delegate) {
      super(1, "prefix", true);
      this.delegate = delegate;
    }

    @Override
    public Future<?> submit(Runnable task) {
      if (rejections.getAndUpdate(r -> r < REJECTION_COUNT ? r + 1 : r) < REJECTION_COUNT) {
        throw new RejectedExecutionException();
      } else {
        accepted.incrementAndGet();
        return delegate.submit(task);
      }
    }

    @Override
    public Future<?> submit(Callable task) {
      if (rejections.getAndUpdate(r -> r < REJECTION_COUNT ? r + 1 : r) < REJECTION_COUNT) {
        throw new RejectedExecutionException();
      } else {
        accepted.incrementAndGet();
        return delegate.submit(task);
      }
    }

    public int getRejections() {
      return rejections.get();
    }

    public int getAccepted() {
      return accepted.get();
    }

    public void reset() {
      rejections.set(0);
      accepted.set(0);
    }
  }
}
