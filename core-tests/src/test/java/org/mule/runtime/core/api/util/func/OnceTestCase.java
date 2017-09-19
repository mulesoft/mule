/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

@SmallTest
public class OnceTestCase extends AbstractMuleTestCase {

  @Test
  public void runOnlyOnce() {
    CountingRunnable runnable = new CountingRunnable();
    Once once = Once.of(runnable);

    once.runOnce();
    once.runOnce();

    assertThat(runnable.getInvokationCount(), is(1));
  }

  @Test
  public void runUntilSuccessful() {
    CountingRunnable runnable = new CountingRunnable();
    Once once = Once.of(() -> {
      runnable.runChecked();
      int count = runnable.getInvokationCount();
      if (count < 3) {
        throw new RuntimeException();
      }
    });

    for (int i = 0; i < 5; i++) {
      try {
        once.runOnce();
        break;
      } catch (Exception e) {

      }
    }

    assertThat(runnable.getInvokationCount(), is(3));
  }

  @Test
  public void concurrentOnce() {
    Latch controlLath = new Latch();
    Latch testLath = new Latch();

    CountingRunnable runnable = new CountingRunnable();
    Once once = Once.of(runnable);

    new Thread(() -> {
      await(controlLath);
      once.runOnce();
    }).start();

    new Thread(() -> {
      controlLath.release();
      once.runOnce();
      testLath.release();
    }).start();

    await(testLath);

    assertThat(runnable.getInvokationCount(), is(1));
  }

  private void await(Latch latch) {
    try {
      assertThat(latch.await(5, SECONDS), is(true));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private class CountingRunnable implements CheckedRunnable {

    private AtomicInteger invokationCount = new AtomicInteger(0);

    @Override
    public void runChecked() throws Exception {
      invokationCount.addAndGet(1);
    }

    public int getInvokationCount() {
      return invokationCount.get();
    }
  }
}
