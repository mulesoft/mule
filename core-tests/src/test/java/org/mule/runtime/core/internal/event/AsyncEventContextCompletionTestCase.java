/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;
import static org.mule.tck.probe.PollingProber.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AsyncEventContextCompletionTestCase extends AbstractMuleTestCase {

  private ExecutorService executorService;

  @Before
  public void setup() {
    executorService = newCachedThreadPool();
  }

  @After
  public void after() {
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Test
  @Issue("W-16640678")
  public void childContextCompletesAsyncAfterParentContext() throws Exception {
    final CompletableFuture<Void> parentTermination = new CompletableFuture<>();
    final Latch testLatch = new Latch();
    final Latch parentCompletedLatch = new Latch();
    final Latch childTerminatedLatch = new Latch();

    BaseEventContext parentContext =
        (BaseEventContext) create("id", "server", mock(ComponentLocation.class), "correlation", of(parentTermination));
    BaseEventContext childContext = DefaultEventContext.child(parentContext, empty());

    parentContext.onComplete((e, t) -> {
      parentCompletedLatch.countDown();
      await(childTerminatedLatch);
      executorService.submit(() -> parentTermination.complete(null));
    });

    parentContext.onTerminated((e, t) -> executorService.submit(() -> testLatch.release()));

    childContext.onComplete((e, t) -> {
      executorService.submit(() -> {
        parentContext.success();
      });
      await(parentCompletedLatch);
    });

    childContext.onTerminated((e, t) -> childTerminatedLatch.countDown());

    executorService.submit(() -> childContext.success());

    await(testLatch);

    check(RECEIVE_TIMEOUT, 500, () -> {
      assertThat("parent did not terminate", parentContext.isTerminated(), is(true));
      assertThat("child did not terminate", childContext.isTerminated(), is(true));
      return true;
    });
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(RECEIVE_TIMEOUT, SECONDS)) {
        throw new AssertionError("Latch timed out");
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
