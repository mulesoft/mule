/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Represents the structure of a project, providing what is needed in order to create its {@link ArtifactDescriptor} with a
 * {@link ArtifactDescriptorFactory}.
 * 
 * @since 4.5
 */
public final class DeployableProjectModel<T extends MuleDeployableModel> {

  private final List<String> exportedPackages;
  private final List<String> exportedResources;
  private final List<Artifact> dependencies;
  private final List<Plugin> additionalPluginDependencies;
  private final Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies;
  private final ArtifactCoordinates artifactCoordinates;
  private final File projectFolder;
  private final List<BundleDependency> appBundleDependencies;
  private final BundleDescriptor bundleDescriptor;
  private final Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies;
  private final Map<BundleDescriptor, List<String>> pluginsExportedPackages;
  private final Map<BundleDescriptor, List<String>> pluginsExportedResources;
  private final T muleDeployableModel;

  public DeployableProjectModel(List<String> exportedPackages, List<String> exportedResources, List<Artifact> dependencies,
                                List<Plugin> additionalPluginDependencies,
                                Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies,
                                ArtifactCoordinates artifactCoordinates, File projectFolder,
                                List<BundleDependency> appBundleDependencies, BundleDescriptor bundleDescriptor,
                                Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies,
                                Map<BundleDescriptor, List<String>> pluginsExportedPackages,
                                Map<BundleDescriptor, List<String>> pluginsExportedResources, T muleDeployableModel) {
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.dependencies = dependencies;
    this.additionalPluginDependencies = additionalPluginDependencies;
    this.pluginsDependencies = pluginsDependencies;
    this.artifactCoordinates = artifactCoordinates;
    this.projectFolder = projectFolder;
    this.appBundleDependencies = appBundleDependencies;
    this.bundleDescriptor = bundleDescriptor;
    this.pluginsBundleDependencies = pluginsBundleDependencies;
    this.pluginsExportedPackages = pluginsExportedPackages;
    this.pluginsExportedResources = pluginsExportedResources;
    this.muleDeployableModel = muleDeployableModel;
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

  public Map<ArtifactCoordinates, List<Artifact>> getPluginsDependencies() {
    return pluginsDependencies;
  }

  public List<Plugin> getAdditionalPluginDependencies() {
    return additionalPluginDependencies;
  }

  public ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

  public File getProjectFolder() {
    return projectFolder;
  }

  public List<BundleDependency> getAppBundleDependencies() {
    return appBundleDependencies;
  }

  public BundleDescriptor getBundleDescriptor() {
    return bundleDescriptor;
  }

  public Map<BundleDescriptor, List<BundleDependency>> getPluginsBundleDependencies() {
    return pluginsBundleDependencies;
  }

  public T getMuleDeployableModel() {
    return muleDeployableModel;
  }

  public Map<BundleDescriptor, List<String>> getPluginsExportedPackages() {
    return pluginsExportedPackages;
  }

  public Map<BundleDescriptor, List<String>> getPluginsExportedResources() {
    return pluginsExportedResources;
  }
}
