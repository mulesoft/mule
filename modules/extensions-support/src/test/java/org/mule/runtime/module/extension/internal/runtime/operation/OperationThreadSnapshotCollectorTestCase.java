/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.util.concurrent.Latch;

import java.util.concurrent.ExecutorService;

public class OperationThreadSnapshotCollectorTestCase {

  private static final ExecutorService executor = newSingleThreadExecutor();
  private static final long TEST_TIME = 10L;
  private static final long TOO_SHORT_TIME = 1L;

  private final Object theSharedLock = new Object();
  private OperationThreadSnapshotCollector collector;

  @Before
  public void setup() {
    collector = new OperationThreadSnapshotCollector();
  }

  @Test
  public void threadIsWaitingInSleep() throws InterruptedException {
    sleep(TEST_TIME);
    OperationThreadSnapshot snapshot = collector.collect();
    assertThat(snapshot.getWaitedTime(), is(greaterThanOrEqualTo(TEST_TIME)));
  }

  @Test
  public void threadWaitedButItDidNotWasteTimeTryingToLock() throws InterruptedException {
    synchronized (theSharedLock) {
      sleep(TEST_TIME);
    }
    OperationThreadSnapshot snapshot = collector.collect();
    assertThat(snapshot.getBlockedTime(), is(lessThanOrEqualTo(TOO_SHORT_TIME)));
  }

  @Test
  public void threadIsBlockedWhileTryingToAcquireALockThatIsOwnedByAnotherThread() throws InterruptedException {
    // Latches used to control the other thread.
    final Latch startLatch = new Latch();
    final Latch terminationLatch = new Latch();

    // The thread that will block trying to acquire the shared lock.
    executor.submit(() -> {
      try {
        startLatch.await();
      } catch (InterruptedException e) {
        fail("Await operation has been interrupted");
      }
      synchronized (theSharedLock) {
        OperationThreadSnapshot snapshot = collector.collect();
        assertThat(snapshot.getBlockedTime(), is(greaterThanOrEqualTo(TEST_TIME)));
        terminationLatch.release();
      }
    });

    // The test thread is the "other" thread mentioned in the test name.
    synchronized (theSharedLock) {
      startLatch.release();
      sleep(TEST_TIME);
    }

    // Wait for the blocked thread assertions before terminating the test.
    terminationLatch.await();
  }

  @Test
  public void cpuTime() {
    for (long l = 0L; l < 100L; ++l) {
      // Just the loop...
    }
    OperationThreadSnapshot snapshot = collector.collect();
    assertThat(snapshot.getCpuTime(), is(not(0L)));
  }
}
