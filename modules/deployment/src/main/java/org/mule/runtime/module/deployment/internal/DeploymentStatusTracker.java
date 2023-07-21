/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.module.deployment.api.DeploymentListener;

/**
 * Keeps track of the deployment status of all artifact in the Mule instance.
 */
public class DeploymentStatusTracker implements DeploymentListener {

  private ArtifactDeploymentStatusTracker applicationDeploymentStatusTracker = new ArtifactDeploymentStatusTracker();
  private ArtifactDeploymentStatusTracker domainDeploymentStatusTracker = new ArtifactDeploymentStatusTracker();

  public ArtifactDeploymentStatusTracker getApplicationDeploymentStatusTracker() {
    return applicationDeploymentStatusTracker;
  }

  public ArtifactDeploymentStatusTracker getDomainDeploymentStatusTracker() {
    return domainDeploymentStatusTracker;
  }
}
