/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.classloader;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.classloader.model.Plugin;
import org.mule.tools.api.classloader.model.SharedLibraryDependency;

import java.util.Collection;
import java.util.List;

/**
 * Represents the structure of a project, providing what is needed in order to create its {@link ArtifactDescriptor} with a
 * {@link ArtifactDescriptorFactory}.
 * 
 * @since 4.5
 */
public final class DeployableProjectModel {

  private final List<String> exportedPackages;
  private final List<String> exportedResources;
  private final List<SharedLibraryDependency> sharedLibraries;
  private final List<Artifact> dependencies;
  private final List<Plugin> additionalPluginDependencies;
  private final Collection<ClassLoaderModel> pluginsClassLoaderModels;
  private final ArtifactCoordinates artifactCoordinates;

  public DeployableProjectModel(List<String> exportedPackages, List<String> exportedResources,
                                List<SharedLibraryDependency> sharedLibraries, List<Artifact> dependencies,
                                List<Plugin> additionalPluginDependencies, Collection<ClassLoaderModel> pluginsClassLoaderModels,
                                ArtifactCoordinates artifactCoordinates) {
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.sharedLibraries = sharedLibraries;
    this.dependencies = dependencies;
    this.additionalPluginDependencies = additionalPluginDependencies;
    this.pluginsClassLoaderModels = pluginsClassLoaderModels;
    this.artifactCoordinates = artifactCoordinates;
  }

  /**
   * If an empty result is returned, it means that everything will be exported.
   * 
   * @return the packages configured by the project developer to be exported.
   */
  public List<String> getExportedPackages() {
    return exportedPackages;
  }

  /**
   * If an empty result is returned, it means that everything will be exported.
   * 
   * @return the resources configured by the project developer to be exported.
   */
  public List<String> getExportedResources() {
    return exportedResources;
  }

  public List<Artifact> getProjectDependencies() {
    return dependencies;
  }

  public Collection<ClassLoaderModel> getPluginsClassLoaderModels() {
    return pluginsClassLoaderModels;
  }

  public List<SharedLibraryDependency> getSharedLibraries() {
    return sharedLibraries;
  }

  public List<Plugin> getAdditionalPluginDependencies() {
    return additionalPluginDependencies;
  }

  public ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

}
