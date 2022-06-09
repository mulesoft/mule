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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Represents the structure of a project, providing what is needed in order to create its {@link ArtifactDescriptor} with a
 * {@link DeployableArtifactDescriptorFactory}.
 * 
 * @since 4.5
 */
public final class DeployableProjectModel {

  private final List<String> packages;
  private final List<String> resources;
  private final Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies;
  private final ArtifactCoordinates artifactCoordinates;
  private final File projectFolder;
  private final List<BundleDependency> deployableBundleDependencies;
  private final Set<BundleDescriptor> sharedDeployableBundleDescriptors;
  private final Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies;
  private final BundleDescriptor bundleDescriptor;
  private final Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies;

  public DeployableProjectModel(List<String> packages,
                                List<String> resources,
                                Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies,
                                ArtifactCoordinates artifactCoordinates,
                                File projectFolder,
                                List<BundleDependency> deployableBundleDependencies,
                                Set<BundleDescriptor> sharedDeployableBundleDescriptors,
                                Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies,
                                BundleDescriptor bundleDescriptor,
                                Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies) {
    this.packages = ImmutableList.copyOf(packages);
    this.resources = ImmutableList.copyOf(resources);
    this.pluginsDependencies = ImmutableMap.copyOf(pluginsDependencies);
    this.artifactCoordinates = artifactCoordinates;
    this.projectFolder = projectFolder;
    this.deployableBundleDependencies = ImmutableList.copyOf(deployableBundleDependencies);
    this.sharedDeployableBundleDescriptors = ImmutableSet.copyOf(sharedDeployableBundleDescriptors);
    this.additionalPluginDependencies = ImmutableMap.copyOf(additionalPluginDependencies);
    this.bundleDescriptor = bundleDescriptor;
    this.pluginsBundleDependencies = ImmutableMap.copyOf(pluginsBundleDependencies);
  }

  public List<String> getPackages() {
    return packages;
  }

  public List<String> getResources() {
    return resources;
  }

  public Map<ArtifactCoordinates, List<Artifact>> getPluginsDependencies() {
    return pluginsDependencies;
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

  public Set<BundleDescriptor> getSharedDeployableBundleDescriptors() {
    return sharedDeployableBundleDescriptors;
  }

  public Map<BundleDescriptor, List<BundleDependency>> getAdditionalPluginDependencies() {
    return additionalPluginDependencies;
  }

  public BundleDescriptor getBundleDescriptor() {
    return bundleDescriptor;
  }

  public Map<BundleDescriptor, List<BundleDependency>> getPluginsBundleDependencies() {
    return pluginsBundleDependencies;
  }

}
