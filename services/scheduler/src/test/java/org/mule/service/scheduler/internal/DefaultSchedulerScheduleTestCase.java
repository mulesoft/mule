/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@RunWith(Parameterized.class)
@Features("Scheduler Task Scheduling")
public class DefaultSchedulerScheduleTestCase extends BaseDefaultSchedulerTestCase {

  private Function<DefaultSchedulerScheduleTestCase, ScheduledExecutorService> executorFactory;

  public DefaultSchedulerScheduleTestCase(Function<DefaultSchedulerScheduleTestCase, ScheduledExecutorService> executorFactory) {
    this.executorFactory = executorFactory;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {(Function<DefaultSchedulerScheduleTestCase, ScheduledExecutorService>) test -> test.createScheduledSameThreadExecutor()},
        {(Function<DefaultSchedulerScheduleTestCase, ScheduledExecutorService>) test -> test
            .createScheduledNotSameThreadExecutor()}
    });
  }

  @Test
  @Description("Tests scheduling a Runnable in the future")
  public void scheduleRunnable() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      latch2.countDown();
      awaitLatch(latch1);
    }, 1, SECONDS);

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    latch1.countDown();
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling get on a ScheduledFuture with a time lower than the duration of the Runnable task throws a TimeoutException")
  public void scheduleRunnableGetTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);

    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      awaitLatch(latch1);
    }, 1, SECONDS);

    expected.expect(TimeoutException.class);

    scheduled.get(EXECUTOR_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests scheduling a Callable in the future")
  public void scheduleCallable() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final long scheduleNanos = System.nanoTime();
    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      latch2.countDown();
      return awaitLatch(latch1);
    }, 1, SECONDS);

    assertThat(latch2.await(2 * EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    latch1.countDown();
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling get on a ScheduledFuture with a time lower than the duration of the Callable task throws a TimeoutException")
  public void scheduleCallableGetTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);

    final ScheduledFuture<?> scheduled = executor.schedule(() -> {
      return awaitLatch(latch1);
    }, 1, SECONDS);

    expected.expect(TimeoutException.class);

    scheduled.get(EXECUTOR_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler with a Runnable scheduled in the future will wait for that task to finish")
  public void scheduleRunnableShutdownBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

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
    latch1.countDown();
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler with a Callable scheduled in the future will wait for that task to finish")
  public void scheduleCallableShutdownBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

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
    latch1.countDown();
    scheduled.get(2 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    final long finishNanos = System.nanoTime();
    assertThat(finishNanos - scheduleNanos, greaterThanOrEqualTo(SECONDS.toNanos(1)));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a Runnable scheduled in the future will cancel that task")
  public void scheduleRunnableShutdownNowBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    executor.schedule(() -> {
      fail("Called after shutdown");
    }, 1, SECONDS);

    assertThat(executor.shutdownNow(), hasSize(1));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a Callable scheduled in the future will cancel that task")
  public void scheduleCallableShutdownNowBeforeFire() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    executor.schedule(() -> {
      fail("Called after shutdown");
    }, 1, SECONDS);

    assertThat(executor.shutdownNow(), hasSize(1));
  }

  @Override
  protected ScheduledExecutorService createExecutor() {
    return executorFactory.apply(this);
  }

  protected ScheduledExecutorService createScheduledSameThreadExecutor() {
    return new DefaultScheduler(sharedExecutor, sharedScheduledExecutor, true);
  }

  protected ScheduledExecutorService createScheduledNotSameThreadExecutor() {
    return new DefaultScheduler(sharedExecutor, sharedScheduledExecutor, false);
  }
}
