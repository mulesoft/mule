/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.classloadermodel;

import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link ClassloaderModel}
 *
 * @since 4.0
 */
public class DefaultClassloaderModel implements ClassloaderModel {

  private Optional<URL> runtimeClasses;
  private URL[] runtimeLibs;
  private Set<String> exportedPackages;
  private Set<String> exportedResources;
  private Set<ArtifactDependency> dependencies;

  public DefaultClassloaderModel(Optional<URL> runtimeClasses, URL[] runtimeLibs, Set<String> exportedPackages,
                                 Set<String> exportedResources,
                                 Set<ArtifactDependency> dependencies) {
    this.runtimeClasses = runtimeClasses;
    this.runtimeLibs = runtimeLibs;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.dependencies = dependencies;
  }

  @Override
  public Optional<URL> getRuntimeClasses() {
    return runtimeClasses;
  }

  @Override
  public URL[] getRuntimeLibs() {
    return runtimeLibs;
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
