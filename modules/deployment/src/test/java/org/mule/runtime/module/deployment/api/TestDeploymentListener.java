/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
