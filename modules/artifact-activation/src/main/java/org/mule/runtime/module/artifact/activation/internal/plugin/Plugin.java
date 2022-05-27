/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import java.util.List;

import org.apache.maven.model.Dependency;

/**
 * Bean to model a plugin that will declare additional dependencies.
 */
public class Plugin {

  private String groupId;

  private String artifactId;

  private List<Dependency> additionalDependencies;

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public List<Dependency> getAdditionalDependencies() {
    return additionalDependencies;
  }

  public void setAdditionalDependencies(List<Dependency> dependencies) {
    this.additionalDependencies = dependencies;
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId;
  }
}
