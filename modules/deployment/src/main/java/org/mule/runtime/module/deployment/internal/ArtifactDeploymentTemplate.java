/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.module.artifact.api.Artifact;

/**
 * Utility to hook callbacks just before and after an artifact is redeployed in Mule.
 */
public interface ArtifactDeploymentTemplate {

  /**
   * No-op implementation of {@link ArtifactDeploymentTemplate}.
   */
  public final ArtifactDeploymentTemplate NOP_ARTIFACT_DEPLOYMENT_TEMPLATE = new ArtifactDeploymentTemplate() {

    @Override
    public void preRedeploy(Artifact artifact) {
      // Nothing to do
    }

    @Override
    public void postRedeploy(Artifact artifact) {
      // Nothing to do
    }
  };

  /**
   * Callback to be invoked just before redeploying an artifact.
   * 
   * @param artifact the artifact that is about to be redeployed.
   */
  void preRedeploy(Artifact artifact);

  /**
   * Callback to be invoked just after redeploying an artifact.
   * 
   * @param artifact the artifact that was just redeployed.
   */
  void postRedeploy(Artifact artifact);

}
