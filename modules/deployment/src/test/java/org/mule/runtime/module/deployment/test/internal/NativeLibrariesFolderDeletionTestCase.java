/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppNativeLibrariesTempFolder;

import static java.lang.String.valueOf;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.NativeLibrariesFolderDeletion;
import org.mule.runtime.module.deployment.internal.RetryScheduledFolderDeletionTask;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Issue("W-15894519")
public class NativeLibrariesFolderDeletionTestCase extends AbstractMuleTestCase {

  private static final int TIMEOUT_MILLIS = 60000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final int CORE_POOL_SIZE = 1;
  private static final int MAX_ATTEMPTS = 3;
  private static final int INITIAL_DELAY = 0;
  private static final int DELAY = 1;

  private static final String ARTIFACT_ID = "application-test";

  private static final TestLogger retryScheduledFolderDeletionTaskLogger = getTestLogger(RetryScheduledFolderDeletionTask.class);

  @Rule
  public TemporaryFolder nativeLibraryFolder = new TemporaryFolder();

  @Test
  public void undeployDeletesTheNativeLibrariesTempFolder() {
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = getDeployer();
    Application application = createMockApplication();
    File nativeLibrariesTempFolder = getNativeLibrariesFolder(application);

    nativeLibrariesTempFolder.mkdirs();

    deployer.setDeploymentListener(mock(DeploymentListener.class));
    deployer.deployArtifact(application, empty());
    assertTrue(nativeLibrariesTempFolder.exists());

    deployer.undeployArtifact(ARTIFACT_ID);
    assertFalse(nativeLibrariesTempFolder.exists());
  }

  @Test
  public void retryScheduledFileDeletionTaskNeverDeletesTheTempFolder() {
    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS,
                                             new TestNeverNativeLibrariesFileDeletion(ARTIFACT_ID,
                                                                                      nativeLibraryFolder.getRoot()));
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        assertTrue(scheduler.isShutdown());
        assertTrue(nativeLibraryFolder.getRoot().exists());
        assertNativeLibrariesTempFolderIsNotDeleteLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  @Test
  public void retryScheduledFileDeletionTaskAtFirstAttemptDeletesTheTempFolder() {
    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS,
                                             new NativeLibrariesFolderDeletion(ARTIFACT_ID,
                                                                               nativeLibraryFolder.getRoot()));
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        assertFalse(nativeLibraryFolder.getRoot().exists());
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
  public void retryScheduledFileDeletionTaskAtSecondAttemptDeletesTheTempFolder() {
    ScheduledExecutorService scheduler = newScheduledThreadPool(CORE_POOL_SIZE);
    RetryScheduledFolderDeletionTask retryTask =
        new RetryScheduledFolderDeletionTask(scheduler, MAX_ATTEMPTS,
                                             new TestAtSecondAttemptNativeLibrariesFileDeletion(ARTIFACT_ID,
                                                                                                nativeLibraryFolder.getRoot()));
    scheduler.scheduleWithFixedDelay(retryTask, INITIAL_DELAY, DELAY, SECONDS);

    Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertTrue(scheduler.isShutdown());
        assertFalse(nativeLibraryFolder.getRoot().exists());
        assertNativeLibrariesTempFolderIsDeleteAtSecondAttemptLogging();
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to test the deletion task.";
      }
    });
  }

  private static class TestNeverNativeLibrariesFileDeletion extends NativeLibrariesFolderDeletion {

    public TestNeverNativeLibrariesFileDeletion(String applicationName, File appNativeLibrariesFolder) {
      super(applicationName, appNativeLibrariesFolder);
    }

    @Override
    public boolean doAction() {
      return false;
    }
  }

  private static class TestAtSecondAttemptNativeLibrariesFileDeletion extends NativeLibrariesFolderDeletion {

    private final int MAX_ATTEMPTS = 2;
    private final AtomicInteger attempts = new AtomicInteger(0);

    public TestAtSecondAttemptNativeLibrariesFileDeletion(String applicationName, File appNativeLibrariesFolder) {
      super(applicationName, appNativeLibrariesFolder);
    }

    @Override
    public boolean doAction() {
      int attempt = attempts.incrementAndGet();
      if (attempt < MAX_ATTEMPTS) {
        return false;
      } else {
        return super.doAction();
      }
    }
  }

  private static DefaultArchiveDeployer<ApplicationDescriptor, Application> getDeployer() {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = createDeployer(artifactDeployer, artifactFactory);
    return deployer;
  }

  private static DefaultArchiveDeployer createDeployer(ArtifactDeployer artifactDeployer,
                                                       AbstractDeployableArtifactFactory artifactFactory) {
    return new DefaultArchiveDeployer(artifactDeployer, artifactFactory, new ObservableList(), null, null);
  }

  private Application createMockApplication() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID);
    when(descriptor.getLoadedNativeLibrariesFolderName()).thenReturn(valueOf(randomUUID()));
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    when(artifact.getResourceFiles()).thenReturn(new File[0]);
    return artifact;
  }

  private File getNativeLibrariesFolder(Application application) {
    String loadedNativeLibrariesFolderName = application.getDescriptor().getLoadedNativeLibrariesFolderName();
    return getAppNativeLibrariesTempFolder(ARTIFACT_ID, loadedNativeLibrariesFolderName);
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
