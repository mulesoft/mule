/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.runtime.api.artifact.Registry;

public class TestDeploymentListener implements DeploymentListener {

  private String artifactName;

  private Registry registry;

  @Override
  public void onArtifactInitialised(String artifactName, Registry registry) {
    this.artifactName = artifactName;
    this.registry = registry;
  }

  @Override
  public void onDeploymentSuccess(String artifactName) {
    this.artifactName = artifactName;
  }

  public String getArtifactName() {
    return artifactName;
  }

  public Registry getRegistry() {
    return registry;
  }
}
