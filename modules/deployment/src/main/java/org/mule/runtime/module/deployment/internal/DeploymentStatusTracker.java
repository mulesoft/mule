/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
