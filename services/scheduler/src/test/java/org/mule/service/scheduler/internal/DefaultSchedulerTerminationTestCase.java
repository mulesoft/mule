/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@RunWith(Parameterized.class)
@Features("Scheduler Termination")
public class DefaultSchedulerTerminationTestCase extends BaseDefaultSchedulerTestCase {

  private Matcher<ExecutorService> terminatedMatcher;

  public DefaultSchedulerTerminationTestCase(Matcher<ExecutorService> terminatedMatcher) {
    this.terminatedMatcher = terminatedMatcher;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {isTerminated()},
        {isTerminatedAfterAwait()}
    });
  }

  @Test
  @Description("Tests that the Scheduler is properly terminated after calling shutdown()")
  public void terminatedAfterShutdownSameExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    executor.shutdown();

    assertThat(executor, terminatedMatcher);
  }

  @Test
  @Description("Tests that calling shutdown() in a Scheduler has no impact on another Scheduler backed by the same Executor")
  public void terminatedAfterShutdownOtherExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor1 = createExecutor();
    final ScheduledExecutorService executor2 = createExecutor();

    executor1.shutdown();

    assertThat(executor1, terminatedMatcher);
    assertThat(executor2, not(terminatedMatcher));
  }

  @Test
  @Description("Tests that the Scheduler is properly terminated after calling shutdownNow()")
  public void terminatedAfterShutdownNowSameExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    executor.shutdownNow();

    assertThat(executor, terminatedMatcher);
  }

  @Test
  @Description("Tests that calling shutdownNow() in a Scheduler has no impact on another Scheduler backed by the same Executor")
  public void terminatedAfterShutdownNowOtherExecutor() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor1 = createExecutor();
    final ScheduledExecutorService executor2 = createExecutor();

    executor1.shutdownNow();

    assertThat(executor1, isTerminated());
    assertThat(executor2, not(terminatedMatcher));
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler while it's running a task waits for it to finish before terminating")
  public void terminatedAfterShutdownRunningTask() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    final Future<Boolean> result = executor.submit(() -> {
      return awaitLatch(latch);
    });

    executor.shutdown();

    assertThat(executor, not(terminatedMatcher));
    latch.countDown();
    result.get(EXECUTOR_TIMEOUT_SECS, SECONDS);

    // Due to how threads are scheduled, the termination state of the executor may become available after the getter of the future
    // returns.
    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler terminates it even if it's running a task")
  public void terminatedAfterShutdownNowRunningTask() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    executor.submit(() -> {
      return awaitLatch(latch);
    });

    executor.shutdownNow();

    assertThat(executor, terminatedMatcher);
  }

  @Test
  @Description("Tests that calling shutdown() on a Scheduler with a queued task runs that task before terminating")
  public void terminatedAfterShutdownPendingTask() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    final Future<Boolean> result = executor.submit(() -> {
      return awaitLatch(latch1);
    });
    final Future<Boolean> pendingResult = executor.submit(() -> {
      return awaitLatch(latch2);
    });

    executor.shutdown();

    assertThat(executor, not(terminatedMatcher));
    latch1.countDown();
    assertThat(result.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));
    assertThat(executor, not(terminatedMatcher));
    latch2.countDown();
    assertThat(pendingResult.get(EXECUTOR_TIMEOUT_SECS, SECONDS), is(true));

    // Due to how threads are scheduled, the termination state of the executor may become available after the getter of the future
    // returns.
    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a queued task doesn't wait for that task to run before terminating")
  public void terminatedAfterShutdownNowPendingTask() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    executor.submit(() -> {
      return awaitLatch(latch);
    });
    executor.submit(() -> {
      return awaitLatch(latch);
    });

    executor.shutdownNow();

    assertThat(executor, terminatedMatcher);
  }

  private static Matcher<ExecutorService> isTerminated() {
    return new TypeSafeMatcher<ExecutorService>() {

      private String itemString;

      @Override
      protected boolean matchesSafely(ExecutorService item) {
        this.itemString = item.toString();
        return item.isTerminated();
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendValue(itemString);
      }
    };
  }

  private static Matcher<ExecutorService> isTerminatedAfterAwait() {
    return new TypeSafeMatcher<ExecutorService>() {

      private String itemString;

      @Override
      protected boolean matchesSafely(ExecutorService item) {
        this.itemString = item.toString();
        try {
          return item.awaitTermination(EXECUTOR_TIMEOUT_SECS, SECONDS);
        } catch (InterruptedException e) {
          currentThread().interrupt();
          return false;
        }
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendValue(itemString);
      }
    };
  }
}
