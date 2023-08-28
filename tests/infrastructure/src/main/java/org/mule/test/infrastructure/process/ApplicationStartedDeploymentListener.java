/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.process;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.module.deployment.api.DeploymentListener;

import java.util.concurrent.TimeUnit;

public class ApplicationStartedDeploymentListener implements DeploymentListener {

  public static final int APPLICATION_STARTED_TIMEOUT_IN_SECONDS = 20;

  private Latch applicationStartedLatch = new Latch();

  public void waitUntilApplicationDeployed() {
    try {
      if (!applicationStartedLatch.await(APPLICATION_STARTED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
        throw new RuntimeException("Application didn't start within " + APPLICATION_STARTED_TIMEOUT_IN_SECONDS + " seconds");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onDeploymentStart(String artifactName) {}

  @Override
  public void onDeploymentSuccess(String artifactName) {
    if (artifactName.equals(MuleContextProcessApplication.TEST_APPLICATION_NAME)) {
      applicationStartedLatch.release();
    }
  }
}
