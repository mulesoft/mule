/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
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

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("Scheduler Shutdown")
public class DefaultShutdownSchedulerTestCase extends BaseDefaultSchedulerTestCase {

  @Test
  @Description("Tests that calling shutdown() on a Scheduler while it's running a task waits for it to finish before terminating")
  public void shutdownWhileRunningTasksFromDifferentSources() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor1 = buildExecutor();
    final ScheduledExecutorService executor2 = buildExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result1 = executor1.submit(() -> {
      return awaitLatch(latch);
    });
    final Future<Boolean> result2 = executor2.submit(() -> {
      return awaitLatch(latch);
    });

    executor2.shutdown();

    latch.countDown();

    // Since both tasks where dispatched before calling shutdown, both must finish.
    assertThat(result1.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    assertThat(result2.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a queued task cancels that task")
  public void shutdownNowWhileRunningTasksFromDifferentSources()
      throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor1 = buildExecutor();
    final ScheduledExecutorService executor2 = buildExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result1 = executor1.submit(() -> {
      return awaitLatch(latch);
    });
    final Runnable task2 = () -> {
      awaitLatch(latch);
    };
    executor2.submit(task2);

    final List<Runnable> notStartedTasks = executor2.shutdownNow();

    latch.countDown();

    // Since the first task was sent to executor1, shutting down executor2 must not affect it
    assertThat(result1.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    assertThat(notStartedTasks, hasSize(1));
    assertThat(notStartedTasks, hasItem(sameInstance(task2)));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdown() is rejected")
  public void submitAfterShutdownSameExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = buildExecutor();

    executor.shutdown();

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdown() on another Scheduler is NOT rejected")
  public void submitAfterShutdownOtherExecutor() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor1 = buildExecutor();
    final ScheduledExecutorService executor2 = buildExecutor();

    executor1.shutdown();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result = executor2.submit(() -> {
      return awaitLatch(latch);
    });

    latch.countDown();

    assertThat(result.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdownNow() is rejected")
  public void submitAfterShutdownNowSameExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = buildExecutor();

    final List<Runnable> notStartedTasks = executor.shutdownNow();

    assertThat(notStartedTasks, is(empty()));

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after calling shutdownNow() on another Scheduler is NOT rejected")
  public void submitAfterShutdownNowOtherExecutor() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor1 = buildExecutor();
    final ScheduledExecutorService executor2 = buildExecutor();

    executor1.shutdownNow();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result = executor2.submit(() -> {
      return awaitLatch(latch);
    });

    latch.countDown();

    assertThat(result.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after the service is stopped is rejected")
  public void submitAfterShutdownSharedExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = buildExecutor();

    sharedExecutor.shutdown();

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that a task submitted to a Scheduler after the service is force-stopped is rejected")
  public void submitAfterShutdownNowSharedExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = buildExecutor();

    final List<Runnable> notStartedTasks = sharedExecutor.shutdownNow();

    assertThat(notStartedTasks, is(empty()));

    assertRejected(executor, SUBMIT_EMPTY_RUNNABLE);
  }

  protected void assertRejected(final ScheduledExecutorService executor,
                                final Consumer<ScheduledExecutorService> submitEmptyRunnable) {
    expected.expect(instanceOf(RejectedExecutionException.class));
    expected.expectMessage(is(executor.toString() + " already shutdown"));
    submitEmptyRunnable.accept(executor);
  }
}
