/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.module.deployment.api.DeploymentListener;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the deployment status of the artifact listening to the deployment notifications
 */
public class ArtifactDeploymentStatusTracker implements DeploymentListener {

  public enum DeploymentState {
    // The deployment is in progress
    DEPLOYING,
    // The deployment was finished with a failure
    FAILED,
    // The deployment was successfully finished
    DEPLOYED
  }

  protected Map<String, DeploymentState> deploymentStates = new ConcurrentHashMap<String, DeploymentState>();

  public Map<String, DeploymentState> getDeploymentStates() {
    return Collections.unmodifiableMap(deploymentStates);
  }

  public void onDeploymentStart(String artifactName) {
    deploymentStates.put(artifactName, DeploymentState.DEPLOYING);
  }

  public void onDeploymentSuccess(String artifactName) {
    deploymentStates.put(artifactName, DeploymentState.DEPLOYED);
  }

  public void onDeploymentFailure(String artifactName, Throwable failureCause) {
    deploymentStates.put(artifactName, DeploymentState.FAILED);
  }

}
