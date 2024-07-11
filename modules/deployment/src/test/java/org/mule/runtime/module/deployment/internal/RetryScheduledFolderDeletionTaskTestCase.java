/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletion;
import org.mule.runtime.module.deployment.internal.RetryScheduledFolderDeletionTask;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-15894519")
public class RetryScheduledFolderDeletionTaskTestCase extends AbstractMuleTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final int CORE_POOL_SIZE = 1;
  private static final int MAX_ATTEMPTS = 3;
  private static final int INITIAL_DELAY = 0;
  private static final int DELAY = 1;

  private static final TestLogger retryScheduledFolderDeletionTaskLogger = getTestLogger(RetryScheduledFolderDeletionTask.class);

  @Test
  public void retryScheduledFolderDeletionTaskAtFirstAttemptDeletesTheTempFolder() {
    NativeLibrariesFolderDeletion nativeLibrariesFolderDeletion = mock(NativeLibrariesFolderDeletion.class);
    when(nativeLibrariesFolderDeletion.doAction()).thenReturn(true);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletion);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletion, times(1)).doAction();
        assertNativeLibrariesTempFolderIsDeleteAtFirstAttemptLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  @Test
  public void retryScheduledFolderDeletionTaskAtSecondAttemptDeletesTheTempFolder() {
    NativeLibrariesFolderDeletion nativeLibrariesFolderDeletion = mock(NativeLibrariesFolderDeletion.class);
    when(nativeLibrariesFolderDeletion.doAction()).thenReturn(false).thenReturn(true);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletion);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletion, times(2)).doAction();
        assertNativeLibrariesTempFolderIsDeleteAtSecondAttemptLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  @Test
  public void retryScheduledFolderDeletionTaskNeverDeletesTheTempFolder() {
    NativeLibrariesFolderDeletion nativeLibrariesFolderDeletion = mock(NativeLibrariesFolderDeletion.class);
    when(nativeLibrariesFolderDeletion.doAction()).thenReturn(false);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletion);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletion, times(3)).doAction();
        assertNativeLibrariesTempFolderIsNotDeleteLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  private void assertNativeLibrariesTempFolderIsNotDeleteLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(MAX_ATTEMPTS, loggingEvents.size());
    assertTrue(loggingEvents.get(0).getMessage().contains("Attempt 1. Failed to perform the action. Retrying..."));
    assertTrue(loggingEvents.get(1).getMessage().contains("Attempt 2. Failed to perform the action. Retrying..."));
    assertTrue(loggingEvents.get(2).getMessage().contains("Failed to perform the action. No further retries will be made."));
  }

  private void assertNativeLibrariesTempFolderIsDeleteAtFirstAttemptLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(0, loggingEvents.size());
  }

  private void assertNativeLibrariesTempFolderIsDeleteAtSecondAttemptLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(1, loggingEvents.size());
    assertTrue(loggingEvents.get(0).getMessage().contains("Attempt 1. Failed to perform the action. Retrying..."));
  }
}
