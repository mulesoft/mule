/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.TERMINATION;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(Parameterized.class)
@Features(SCHEDULER_SERVICE)
@Stories(TERMINATION)
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
  @Description("Tests that calling shutdownNow() on a Scheduler terminates it even if it's running a submitted task")
  public void terminatedAfterShutdownNowRunningSubmittedTask() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    executor.submit(() -> {
      return awaitLatch(latch);
    });

    executor.shutdownNow();

    assertThat(executor, terminatedMatcher);
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler terminates it even if it's running a task")
  public void terminatedAfterShutdownNowRunningTask() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    executor.execute(() -> {
      awaitLatch(latch);
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
  @Description("Tests that calling shutdown() on a Scheduler with a fixed-delay Callable in-between executions frees the timer executor")
  public void terminatedAfterShutdownInBetweenFixedDelayTask() throws InterruptedException, ExecutionException, TimeoutException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    final ScheduledFuture<?> result = executor.scheduleWithFixedDelay(() -> {
      latch.countDown();
    }, 0, 10, SECONDS);

    latch.await();
    result.cancel(false);

    assertTerminationIsNotDelayed(sharedScheduledExecutor);

    executor.shutdown();
    // Due to how threads are scheduled, the termination state of the executor may become available after the getter of the future
    // returns.
    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a queued submitted task doesn't wait for that task to run before terminating")
  public void terminatedAfterShutdownNowPendingSubmittedTask() throws InterruptedException, ExecutionException {
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

  @Test
  @Description("Tests that calling shutdownNow() on a Scheduler with a queued task doesn't wait for that task to run before terminating")
  public void terminatedAfterShutdownNowPendingTask() throws InterruptedException, ExecutionException {
    final ScheduledExecutorService executor = createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    executor.execute(() -> {
      awaitLatch(latch);
    });
    executor.execute(() -> {
      awaitLatch(latch);
    });

    executor.shutdownNow();

    assertThat(executor, terminatedMatcher);
  }

  @Test
  @Description("Tests that the Scheduler is gracefully terminated after calling stop()")
  public void terminatedAfterStopGracefully() throws InterruptedException, ExecutionException {
    final Scheduler executor = (Scheduler) createExecutor();

    sharedExecutor.submit(() -> executor.stop(10, SECONDS));

    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler while it's running a submitted task waits for it to finish before terminating gracefully")
  public void terminatedAfterStopGracefullyRunningSubmittedTask()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    executor.submit(() -> {
      latch1.countDown();
      return awaitLatch(latch2);
    });

    latch1.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      auxExecutor.submit(() -> executor.stop(10, SECONDS));
      latch2.countDown();

      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler while it's running a task waits for it to finish before terminating gracefully")
  public void terminatedAfterStopGracefullyRunningTask() throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    executor.execute(() -> {
      latch1.countDown();
      awaitLatch(latch2);
    });

    latch1.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      auxExecutor.submit(() -> executor.stop(10, SECONDS));
      latch2.countDown();

      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler with a queued submitted task runs that task before terminating gracefully")
  public void terminatedAfterStopGracefullyPendingSubmittedTask()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    executor.submit(() -> {
      return awaitLatch(latch1);
    });
    executor.submit(() -> {
      return awaitLatch(latch2);
    });

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      auxExecutor.submit(() -> executor.stop(10, SECONDS));

      latch1.countDown();
      latch2.countDown();

      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler with a queued task runs that task before terminating gracefully")
  public void terminatedAfterStopGracefullyPendingTask() throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    executor.execute(() -> {
      awaitLatch(latch1);
    });
    executor.execute(() -> {
      awaitLatch(latch2);
    });

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      auxExecutor.submit(() -> executor.stop(10, SECONDS));

      latch1.countDown();
      latch2.countDown();

      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler after running a submitted task terminates gracefullyand immediately")
  public void terminatedAfterStopGracefullyFinishedSubmittedTask()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    executor.submit(() -> {
      return true;
    });

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      final long stopReqNanos = nanoTime();
      auxExecutor.submit(() -> executor.stop(10, SECONDS));
      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
      assertThat(NANOSECONDS.toMillis(nanoTime() - stopReqNanos), lessThan(30l));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler after running a task terminates gracefullyand immediately")
  public void terminatedAfterStopGracefullyFinishedTask() throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    executor.execute(() -> {
    });

    final ExecutorService auxExecutor = newSingleThreadExecutor();
    try {
      final long stopReqNanos = nanoTime();
      auxExecutor.submit(() -> executor.stop(10, SECONDS));
      new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(executor, terminatedMatcher);
        return true;
      }));
      assertThat(NANOSECONDS.toMillis(nanoTime() - stopReqNanos), lessThan(30l));
    } finally {
      auxExecutor.shutdown();
    }
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler while it's running a submitted task forcefully terminates it")
  public void terminatedAfterStopForcefullyRunningSubmittedTask()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    AtomicReference<Thread> taskThread = new AtomicReference<>();

    executor.submit(() -> {
      latch1.countDown();
      try {
        return awaitLatch(latch2);
      } finally {
        if (currentThread().isInterrupted()) {
          taskThread.set(currentThread());
        }
      }
    });

    latch1.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);

    executor.stop(2, SECONDS);

    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(taskThread.get(), is(not(nullValue())));
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler while it's running a task forcefully terminates it")
  public void terminatedAfterStopForcefullyRunningTask() throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);

    AtomicReference<Thread> taskThread = new AtomicReference<>();

    executor.execute(() -> {
      latch1.countDown();
      try {
        awaitLatch(latch2);
      } finally {
        if (currentThread().isInterrupted()) {
          taskThread.set(currentThread());
        }
      }
    });

    latch1.await(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);

    executor.stop(2, SECONDS);

    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(taskThread.get(), is(not(nullValue())));
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler with a queued submitted task forcefully terminates it")
  public void terminatedAfterStopForcefullyPendingSubmittedTask()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch finallyLatch = new CountDownLatch(2);

    AtomicReference<Thread> taskThread = new AtomicReference<>();
    AtomicReference<Thread> pendingTaskThread = new AtomicReference<>();

    executor.submit(() -> {
      try {
        return awaitLatch(latch);
      } finally {
        if (currentThread().isInterrupted()) {
          taskThread.set(currentThread());
        }
        finallyLatch.countDown();
      }
    });
    executor.submit(() -> {
      try {
        return awaitLatch(latch);
      } finally {
        if (currentThread().isInterrupted()) {
          pendingTaskThread.set(currentThread());
        }
        finallyLatch.countDown();
      }
    });

    executor.stop(2, SECONDS);

    finallyLatch.await(2, SECONDS);
    assertThat(taskThread.get(), is(not(nullValue())));
    assertThat(pendingTaskThread.get(), is(nullValue()));

    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(executor, terminatedMatcher);
      return true;
    }));
  }

  @Test
  @Description("Tests that calling stop() on a Scheduler with a queued task forcefully terminates it")
  public void terminatedAfterStopForcefullyPendingTask() throws InterruptedException, ExecutionException, TimeoutException {
    final Scheduler executor = (Scheduler) createExecutor();

    final CountDownLatch latch = new CountDownLatch(1);

    AtomicReference<Thread> taskThread = new AtomicReference<>();
    AtomicReference<Thread> pendingTaskThread = new AtomicReference<>();

    executor.execute(() -> {
      try {
        awaitLatch(latch);
      } finally {
        if (currentThread().isInterrupted()) {
          taskThread.set(currentThread());
        }
      }
    });
    executor.execute(() -> {
      try {
        awaitLatch(latch);
      } finally {
        if (currentThread().isInterrupted()) {
          pendingTaskThread.set(currentThread());
        }
      }
    });

    executor.stop(2, SECONDS);

    new PollingProber(100, 10).check(new JUnitLambdaProbe(() -> {
      assertThat(taskThread.get(), is(not(nullValue())));
      assertThat(pendingTaskThread.get(), is(nullValue()));
      assertThat(executor, terminatedMatcher);
      return true;
    }));
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
