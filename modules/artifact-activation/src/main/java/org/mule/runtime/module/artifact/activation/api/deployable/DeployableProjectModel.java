/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Represents the structure of a project, providing what is needed in order to create its {@link ArtifactDescriptor} with a
 * {@link DeployableArtifactDescriptorFactory}.
 * 
 * @since 4.5
 */
public final class DeployableProjectModel {

  private final List<String> packages;
  private final List<String> resources;
  private final List<Artifact> dependencies;
  private final List<Plugin> additionalPluginDependencies;
  private final Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies;
  private final ArtifactCoordinates artifactCoordinates;
  private final File projectFolder;
  private final List<BundleDependency> deployableBundleDependencies;
  private final BundleDescriptor bundleDescriptor;
  private final Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies;

  public DeployableProjectModel(List<String> packages, List<String> resources, List<Artifact> dependencies,
                                List<Plugin> additionalPluginDependencies,
                                Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies,
                                ArtifactCoordinates artifactCoordinates, File projectFolder,
                                List<BundleDependency> deployableBundleDependencies, BundleDescriptor bundleDescriptor,
                                Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies) {
    this.packages = ImmutableList.copyOf(packages);
    this.resources = ImmutableList.copyOf(resources);
    this.dependencies = ImmutableList.copyOf(dependencies);
    this.additionalPluginDependencies = ImmutableList.copyOf(additionalPluginDependencies);
    this.pluginsDependencies = ImmutableMap.copyOf(pluginsDependencies);
    this.artifactCoordinates = artifactCoordinates;
    this.projectFolder = projectFolder;
    this.deployableBundleDependencies = ImmutableList.copyOf(deployableBundleDependencies);
    this.bundleDescriptor = bundleDescriptor;
    this.pluginsBundleDependencies = ImmutableMap.copyOf(pluginsBundleDependencies);
  }

  public List<String> getPackages() {
    return packages;
  }

  public List<String> getResources() {
    return resources;
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

  public List<BundleDependency> getDeployableBundleDependencies() {
    return deployableBundleDependencies;
  }

  public BundleDescriptor getBundleDescriptor() {
    return bundleDescriptor;
  }

  public Map<BundleDescriptor, List<BundleDependency>> getPluginsBundleDependencies() {
    return pluginsBundleDependencies;
  }

}
