/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.SHUTDOWN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.Test;
import org.quartz.SchedulerException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(SCHEDULER_SERVICE)
@Stories(SHUTDOWN)
public class DefaultSchedulerShutdownTestCase extends BaseDefaultSchedulerTestCase {

  private ScheduledExecutorService executor;
  private ScheduledExecutorService otherExecutor;

  @Override
  public void before() throws SchedulerException {
    super.before();
    executor = createExecutor();
    otherExecutor = createExecutor();
  }

  @Override
  public void after() throws SchedulerException, InterruptedException {
    executor.shutdownNow();
    otherExecutor.shutdownNow();
    executor.awaitTermination(5, SECONDS);
    otherExecutor.awaitTermination(5, SECONDS);
    super.after();
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler while it's running a task waits for it to finish before terminating")
  public void shutdownWhileRunningTasksFromDifferentSources() throws InterruptedException, ExecutionException, TimeoutException {
    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result1 = executor.submit(() -> {
      return awaitLatch(latch);
    });
    final Future<Boolean> result2 = otherExecutor.submit(() -> {
      return awaitLatch(latch);
    });

    otherExecutor.shutdown();

    latch.countDown();

    // Since both tasks where dispatched before calling shutdown, both must finish.
    assertThat(result1.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    assertThat(result2.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a queued task cancels that task")
  public void shutdownNowWhileRunningTasksFromDifferentSources()
      throws InterruptedException, ExecutionException, TimeoutException {
    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result1 = executor.submit(() -> {
      return awaitLatch(latch);
    });
    final Runnable task2 = () -> {
      awaitLatch(latch);
    };
    otherExecutor.submit(task2);

    final List<Runnable> notStartedTasks = otherExecutor.shutdownNow();

    latch.countDown();

    // Since the first task was sent to executor1, shutting down executor2 must not affect it
    assertThat(result1.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    assertThat(notStartedTasks, hasSize(1));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdown() is rejected")
  public void submitAfterShutdownSameExecutor() throws InterruptedException, ExecutionException {
    executor.shutdown();

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdown() on another Scheduler is NOT rejected")
  public void submitAfterShutdownOtherExecutor() throws InterruptedException, ExecutionException, TimeoutException {
    executor.shutdown();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result = otherExecutor.submit(() -> {
      return awaitLatch(latch);
    });

    latch.countDown();

    assertThat(result.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdownNow() is rejected")
  public void submitAfterShutdownNowSameExecutor() throws InterruptedException, ExecutionException {
    final List<Runnable> notStartedTasks = executor.shutdownNow();

    assertThat(notStartedTasks, is(empty()));

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdownNow() on another Scheduler is NOT rejected")
  public void submitAfterShutdownNowOtherExecutor() throws InterruptedException, ExecutionException, TimeoutException {
    executor.shutdownNow();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result = otherExecutor.submit(() -> {
      return awaitLatch(latch);
    });

    latch.countDown();

    assertThat(result.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after the service is stopped is rejected")
  public void submitAfterShutdownSharedExecutor() throws InterruptedException, ExecutionException {
    sharedExecutor.shutdown();

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after the service is force-stopped is rejected")
  public void submitAfterShutdownNowSharedExecutor() throws InterruptedException, ExecutionException {
    final List<Runnable> notStartedTasks = sharedExecutor.shutdownNow();

    assertThat(notStartedTasks, is(empty()));

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a running task is interrupted when shutdownNow() is called")
  public void shutdownNowInterruptsTask() throws InterruptedException, ExecutionException {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch triggeredLatch = new CountDownLatch(1);
    final CountDownLatch interruptionLatch = new CountDownLatch(1);

    final Future<Boolean> result = executor.submit(() -> {
      triggeredLatch.countDown();
      final boolean awaited = awaitLatch(latch);
      assertThat(Thread.interrupted(), is(true));
      interruptionLatch.countDown();
      return awaited;
    });

    triggeredLatch.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
    final List<Runnable> notStartedTasks = executor.shutdownNow();
    interruptionLatch.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);

    assertThat(notStartedTasks, is(empty()));
    assertThat(result.isCancelled(), is(true));
  }

  protected void assertRejected(final ScheduledExecutorService executor,
                                final Consumer<ScheduledExecutorService> submitEmptyRunnable) {
    expected.expect(instanceOf(RejectedExecutionException.class));
    expected.expectMessage(is(executor.toString() + " already shutdown"));
    submitEmptyRunnable.accept(executor);
  }
}
