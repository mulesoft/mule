/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.descriptor;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.File;
import java.net.URL;

/**
 * Describes an artifact that is deployable on the container
 */
public class DeployableArtifactDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_DEPLOY_PROPERTIES_RESOURCE = "mule-deploy.properties";

  private boolean redeploymentEnabled = true;
  private URL[] runtimeLibs = new URL[0];
  private URL[] sharedRuntimeLibs = new URL[0];
  private File location;

  /**
   * Creates a new deployable artifact descriptor
   *
   * @param name artifact name. Non empty.
   */
  public DeployableArtifactDescriptor(String name) {
    super(name);
  }

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

  /**
   * @param location the directory where the artifact content is stored.
   */
  public void setArtifactLocation(File location) {
    this.location = location;
  }

  /**
   * @return the directory where the artifact content is stored.
     */
  public File getArtifactLocation() {
    return this.location;
  }
}
