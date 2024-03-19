/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.profiling.threading.ThreadSnapshot;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.util.concurrent.Latch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
public class JvmThreadSnapshotCollectorTestCase {

  private ExecutorService executor;
  private static final long TEST_TIME = 10L;

  private final Object theSharedLock = new Object();
  private final ThreadSnapshotCollector threadSnapshotCollector = new JvmThreadSnapshotCollector();

  @Before
  public void setUp() {
    executor = newSingleThreadExecutor();
  }

  @After
  public void tearDown() {
    executor.shutdown();
  }

  @Test
  public void threadIsWaitingInSleep() throws InterruptedException {
    Long previousWaitedTime = threadSnapshotCollector.getCurrentThreadSnapshot().getWaitedTime();
    sleep(TEST_TIME);
    assertThat(threadSnapshotCollector.getCurrentThreadSnapshot().getWaitedTime(),
               is(greaterThanOrEqualTo(TEST_TIME + previousWaitedTime)));
  }

  @Test
  public void threadWaitedButItDidNotWasteThatTimeTryingToLock() throws InterruptedException {
    Long previousBlockedTime = threadSnapshotCollector.getCurrentThreadSnapshot().getBlockedTime();
    synchronized (theSharedLock) {
      sleep(TEST_TIME);
    }
    assertThat(threadSnapshotCollector.getCurrentThreadSnapshot().getBlockedTime(),
               is(lessThan(TEST_TIME + previousBlockedTime)));
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
    Long previousCpuTime = threadSnapshotCollector.getCurrentThreadSnapshot().getCpuTime();
    for (long l = 0L; l < 10000000L; ++l) {
      // Just the loop to make the cpu work for some time...
    }
    assertThat(threadSnapshotCollector.getCurrentThreadSnapshot().getCpuTime(), is(greaterThan(previousCpuTime)));
  }
}
