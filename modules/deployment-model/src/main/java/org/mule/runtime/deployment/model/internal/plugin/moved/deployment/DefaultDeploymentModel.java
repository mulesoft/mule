/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment;

import org.mule.runtime.deployment.model.api.plugin.moved.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;

import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link DeploymentModel}
 *
 * @since 4.0
 */
public class DefaultDeploymentModel implements DeploymentModel {

  private Optional<URL> runtimeClasses = Optional.empty();
  private Set<String> exportedPackages = new HashSet<>();
  private Set<String> exportedResources = new HashSet<>();
  private Set<ArtifactDependency> dependencies = new HashSet<>();

  public DefaultDeploymentModel(Optional<URL> runtimeClasses, Set<String> exportedPackages, Set<String> exportedResources,
                                Set<ArtifactDependency> dependencies) {
    this.runtimeClasses = runtimeClasses;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.dependencies = dependencies;
  }

  @Override
  public Optional<URL> getRuntimeClasses() {
    return runtimeClasses;
  }

  @Override
  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  @Override
  public Set<String> getExportedResources() {
    return exportedResources;
  }

  @Override
  public Set<ArtifactDependency> getDependencies() {
    return dependencies;
  }
}
