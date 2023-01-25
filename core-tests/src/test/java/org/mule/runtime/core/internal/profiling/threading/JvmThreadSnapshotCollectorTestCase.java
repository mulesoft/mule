/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.threading;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.profiling.threading.ThreadSnapshot;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.junit4.FlakyTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
@RunWith(FlakinessDetectorTestRunner.class)
@FlakyTest
public class JvmThreadSnapshotCollectorTestCase extends AbstractMuleTestCase {

  private static final ExecutorService executor = newSingleThreadExecutor();
  private static final long TEST_TIME = 10L;
  private final Object theSharedLock = new Object();
  private final ThreadSnapshotCollector threadSnapshotCollector = new JvmThreadSnapshotCollector();

  @Test
  public void threadIsWaitingInSleep() throws InterruptedException {
    sleep(TEST_TIME);
    ThreadSnapshot snapshot = threadSnapshotCollector.getCurrentThreadSnapshot();
    assertThat(snapshot.getWaitedTime(), is(greaterThanOrEqualTo(TEST_TIME)));
  }

  @Test
  public void threadWaitedButItDidNotWasteThatTimeTryingToLock() throws InterruptedException {
    synchronized (theSharedLock) {
      sleep(TEST_TIME);
    }
    ThreadSnapshot snapshot = threadSnapshotCollector.getCurrentThreadSnapshot();
    assertThat(snapshot.getBlockedTime(), is(lessThan(TEST_TIME)));
  }

  @Test
  public void threadIsBlockedWhileTryingToAcquireALockThatIsOwnedByAnotherThread() throws Exception {
    // Latches used to control the two threads execution.
    final Latch scheduledTaskEndLatch = new Latch();

    // The thread that will block trying to acquire the shared lock.
    Future<ThreadSnapshot> task = executor.submit(() -> {
      try {
        // Wait until the "other" thread acquires theSharedLock.
        scheduledTaskEndLatch.await();
      } catch (InterruptedException e) {
        fail("Await operation has been interrupted");
      }
      // Attempt to acquire the same lock (must cause the thread to block).
      synchronized (theSharedLock) {
        // ThreadSnapshot that must inform blocked time.
        return threadSnapshotCollector.getCurrentThreadSnapshot();
      }
    });

    // The test thread is the "other" thread mentioned in the test name, so it initially acquires theSharedLock.
    synchronized (theSharedLock) {
      scheduledTaskEndLatch.release();
      sleep(TEST_TIME);
    }

    assertThat(task.get().getBlockedTime(), is(greaterThan(0L)));
  }

  @Test
  public void cpuTime() {
    for (long l = 0L; l < 100L; ++l) {
      // Just the loop to make the cpu work for some time...
    }
    ThreadSnapshot snapshot = threadSnapshotCollector.getCurrentThreadSnapshot();
    assertThat(snapshot.getCpuTime(), is(not(0L)));
  }
}
