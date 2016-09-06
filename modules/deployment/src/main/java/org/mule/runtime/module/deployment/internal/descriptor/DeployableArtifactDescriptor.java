/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.descriptor;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.net.URL;

/**
 * Describes an artifact that is deployable on the container
 */
public class DeployableArtifactDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_DEPLOY_PROPERTIES_RESOURCE = "mule-deploy.properties";

  private boolean redeploymentEnabled = true;
  private URL[] runtimeLibs = new URL[0];
  private URL[] sharedRuntimeLibs = new URL[0];

  public boolean isRedeploymentEnabled() {
    return redeploymentEnabled;
  }

  public void setRedeploymentEnabled(boolean redeploymentEnabled) {
    this.redeploymentEnabled = redeploymentEnabled;
  }

  public URL[] getRuntimeLibs() {
    return runtimeLibs;
  }

  public void setRuntimeLibs(URL[] runtimeLibs) {
    this.runtimeLibs = runtimeLibs;
  }

  public URL[] getSharedRuntimeLibs() {
    return sharedRuntimeLibs;
  }

  public void setSharedRuntimeLibs(URL[] sharedRuntimeLibs) {
    this.sharedRuntimeLibs = sharedRuntimeLibs;
  }
}
