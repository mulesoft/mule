/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.EXHAUSTION;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(SCHEDULER_SERVICE)
@Stories(EXHAUSTION)
public class DefaultSchedulerExhaustionTestCase extends BaseDefaultSchedulerTestCase {

  @Test
  @Description("Tests that trying to execute a runnable on a full executor fails due to exhaustion")
  public void failOnExhaustedExecute() {
    final ScheduledExecutorService executor = createExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    failOnExhausted(executor, exec -> exec.execute(() -> awaitLatch(latch)), EXECUTE_EMPTY_RUNNABLE, EXECUTE_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that trying to submit a runnable on a full executor fails due to exhaustion")
  public void failOnExhaustedSubmitRunnable() {
    final ScheduledExecutorService executor = createExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    failOnExhausted(executor, exec -> exec.submit((Runnable) () -> awaitLatch(latch)), SUBMIT_EMPTY_RUNNABLE,
                    SUBMIT_EMPTY_RUNNABLE);
  }

  @Test
  @Description("Tests that trying to submit a callable on a full executor fails due to exhaustion")
  public void failOnExhaustedSubmitCallable() {
    final ScheduledExecutorService executor = createExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    failOnExhausted(executor, exec -> exec.submit(() -> {
      awaitLatch(latch);
      return 0;
    }), SUBMIT_EMPTY_CALLABLE, SUBMIT_EMPTY_CALLABLE);
  }

  @Test
  @Description("Tests that trying to submit a runnable with result on a full executor fails due to exhaustion")
  public void failOnExhaustedSubmitCallableWithResult() {
    final ScheduledExecutorService executor = createExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    failOnExhausted(executor, exec -> exec.submit(() -> awaitLatch(latch), 0), SUBMIT_RESULT_RUNNABLE, SUBMIT_RESULT_RUNNABLE);
  }


  @Test
  public void failOnExhaustedMixed() {
    final ScheduledExecutorService executor = createExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    failOnExhausted(executor, exec -> exec.submit(() -> awaitLatch(latch), 0), EXECUTE_EMPTY_RUNNABLE, SUBMIT_EMPTY_RUNNABLE);
  }

  private void failOnExhausted(ScheduledExecutorService executor, Consumer<ScheduledExecutorService> execute1,
                               Consumer<ScheduledExecutorService> execute2, Consumer<ScheduledExecutorService> execute3) {
    execute1.accept(executor);

    // This one will be queued
    execute2.accept(executor);

    expected.expect(instanceOf(RejectedExecutionException.class));

    // This one will be rejected
    execute3.accept(executor);
  }

}
