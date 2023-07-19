/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentStatusTracker.DeploymentState.DEPLOYED;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentStatusTracker.DeploymentState.DEPLOYING;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentStatusTracker.DeploymentState.FAILED;

import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.module.deployment.api.DeploymentListener;

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

  protected Map<String, DeploymentState> deploymentStates = new ConcurrentHashMap<>();

  public Map<String, DeploymentState> getDeploymentStates() {
    return unmodifiableMap(deploymentStates);
  }

  @Override
  public void onDeploymentStart(String artifactName) {
    deploymentStates.put(artifactName, DEPLOYING);
  }

  @Override
  public void onDeploymentSuccess(String artifactName) {
    deploymentStates.put(artifactName, DEPLOYED);
  }

  @Override
  public void onDeploymentFailure(String artifactName, Throwable failureCause) {
    deploymentStates.put(artifactName, FAILED);
  }

}
