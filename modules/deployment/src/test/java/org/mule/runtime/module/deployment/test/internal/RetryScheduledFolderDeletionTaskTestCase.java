/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletionActionTask;
import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletionRetryScheduledTask;
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

  private static final TestLogger retryScheduledFolderDeletionTaskLogger =
      getTestLogger(NativeLibrariesFolderDeletionRetryScheduledTask.class);

  @Test
  public void retryScheduledFolderDeletionTaskAtFirstAttemptDeletesTheTempFolder() {
    NativeLibrariesFolderDeletionActionTask nativeLibrariesFolderDeletionActionTask =
        mock(NativeLibrariesFolderDeletionActionTask.class);
    when(nativeLibrariesFolderDeletionActionTask.tryAction()).thenReturn(true);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    NativeLibrariesFolderDeletionRetryScheduledTask retryTask =
        new NativeLibrariesFolderDeletionRetryScheduledTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletionActionTask);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletionActionTask, times(1)).tryAction();
        assertNativeLibrariesTempFolderIsDeletedAtFirstAttemptLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  @Test
  public void retryScheduledFolderDeletionTaskAtSecondToLastAttemptDeletesTheTempFolder() {
    NativeLibrariesFolderDeletionActionTask nativeLibrariesFolderDeletionActionTask =
        mock(NativeLibrariesFolderDeletionActionTask.class);
    when(nativeLibrariesFolderDeletionActionTask.tryAction()).thenReturn(false).thenReturn(true);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    NativeLibrariesFolderDeletionRetryScheduledTask retryTask =
        new NativeLibrariesFolderDeletionRetryScheduledTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletionActionTask);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletionActionTask, times(2)).tryAction();
        assertNativeLibrariesTempFolderIsDeletedAtSecondToLastAttemptLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  @Test
  public void retryScheduledFolderDeletionTaskAtLastAttemptDeletesTheTempFolder() {
    NativeLibrariesFolderDeletionActionTask nativeLibrariesFolderDeletionActionTask =
        mock(NativeLibrariesFolderDeletionActionTask.class);
    when(nativeLibrariesFolderDeletionActionTask.tryAction()).thenReturn(false).thenReturn(false).thenReturn(true);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    NativeLibrariesFolderDeletionRetryScheduledTask retryTask =
        new NativeLibrariesFolderDeletionRetryScheduledTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletionActionTask);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletionActionTask, times(3)).tryAction();
        assertNativeLibrariesTempFolderIsDeletedAtLastAttemptLogging();
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
    NativeLibrariesFolderDeletionActionTask nativeLibrariesFolderDeletionActionTask =
        mock(NativeLibrariesFolderDeletionActionTask.class);
    when(nativeLibrariesFolderDeletionActionTask.tryAction()).thenReturn(false);

    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    NativeLibrariesFolderDeletionRetryScheduledTask retryTask =
        new NativeLibrariesFolderDeletionRetryScheduledTask(scheduler, MAX_ATTEMPTS, nativeLibrariesFolderDeletionActionTask);
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        assertTrue(scheduler.isShutdown());
        verify(nativeLibrariesFolderDeletionActionTask, times(3)).tryAction();
        assertNativeLibrariesTempFolderIsNotDeletedLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  private void assertNativeLibrariesTempFolderIsDeletedAtFirstAttemptLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(0, loggingEvents.size());
  }

  private void assertNativeLibrariesTempFolderIsDeletedAtSecondToLastAttemptLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(2, loggingEvents.size());
    assertEquals("Attempt 1. Failed to perform the action. Retrying...", loggingEvents.get(0).getFormattedMessage());
    assertEquals("Attempt 2. System.gc() executed.", loggingEvents.get(1).getFormattedMessage());
  }

  private void assertNativeLibrariesTempFolderIsDeletedAtLastAttemptLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(MAX_ATTEMPTS, loggingEvents.size());
    assertEquals("Attempt 1. Failed to perform the action. Retrying...", loggingEvents.get(0).getFormattedMessage());
    assertEquals("Attempt 2. System.gc() executed.", loggingEvents.get(1).getFormattedMessage());
    assertEquals("Attempt 2. Failed to perform the action. Retrying...", loggingEvents.get(2).getFormattedMessage());
  }

  private void assertNativeLibrariesTempFolderIsNotDeletedLogging() {
    List<LoggingEvent> loggingEvents = retryScheduledFolderDeletionTaskLogger.getAllLoggingEvents();
    assertEquals(4, loggingEvents.size());
    assertEquals("Attempt 1. Failed to perform the action. Retrying...", loggingEvents.get(0).getFormattedMessage());
    assertEquals("Attempt 2. System.gc() executed.", loggingEvents.get(1).getFormattedMessage());
    assertEquals("Attempt 2. Failed to perform the action. Retrying...", loggingEvents.get(2).getFormattedMessage());
    assertEquals("Failed to perform the action. No further retries will be made.", loggingEvents.get(3).getFormattedMessage());
  }
}
