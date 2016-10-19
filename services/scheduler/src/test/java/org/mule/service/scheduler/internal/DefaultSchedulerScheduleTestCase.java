/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("Scheduler Task Scheduling")
public class DefaultSchedulerScheduleTestCase extends BaseDefaultSchedulerTestCase {

  @Test
  @Description("Tests scheduling a Runnable in the future")
  public void scheduleRunnable() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      latch2.countDown();
      awaitLatch(latch1);
    }, 1, SECONDS);

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests scheduling a Callable in the future")
  public void scheduleCallable() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      latch2.countDown();
      return awaitLatch(latch1);
    }, 1, SECONDS);

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler with a Runnable scheduled in the future will wait for that task to finish")
  public void scheduleRunnableShutdownBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      assertThat(executor.isShutdown(), is(true));
      latch2.countDown();
      awaitLatch(latch1);
    }, 1, SECONDS);

    executor.shutdown();

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler with a Callable scheduled in the future will wait for that task to finish")
  public void scheduleCallableShutdownBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      assertThat(executor.isShutdown(), is(true));
      latch2.countDown();
      return awaitLatch(latch1);
    }, 1, SECONDS);

    executor.shutdown();

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a Runnable scheduled in the future will cancel that task")
  public void scheduleRunnableShutdownNowBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      fail("Called after shutdown");
    }, 1, SECONDS);

    assertThat(executor.shutdownNow(), hasSize(1));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a Callable scheduled in the future will cancel that task")
  public void scheduleCallableShutdownNowBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = buildExecutor();

    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      fail("Called after shutdown");
    }, 1, SECONDS);

    assertThat(executor.shutdownNow(), hasSize(1));
  }
}
