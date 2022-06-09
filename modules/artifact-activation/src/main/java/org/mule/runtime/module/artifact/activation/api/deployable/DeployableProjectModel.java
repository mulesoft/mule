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
  private final ArtifactCoordinates artifactCoordinates;
  private final File projectFolder;
  private final List<BundleDependency> dependencies;
  private final Set<BundleDescriptor> sharedLibraries;
  private final Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies;
  private final BundleDescriptor bundleDescriptor;
  private final Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies;

  /**
   * Creates a new instance with the provided parameters.
   * 
   * @param packages                     See {@link #getPackages()}
   * @param resources                    See {@link #getResources()}
   * @param artifactCoordinates          See {@link #getArtifactCoordinates()}
   * @param projectFolder
   * @param dependencies
   * @param sharedLibraries              See {@link #getSharedLibraries()}
   * @param additionalPluginDependencies See {@link #additionalPluginDependencies}
   * @param bundleDescriptor
   * @param pluginsBundleDependencies
   */
  public DeployableProjectModel(List<String> packages,
                                List<String> resources,
                                ArtifactCoordinates artifactCoordinates,
                                File projectFolder,
                                List<BundleDependency> dependencies,
                                Set<BundleDescriptor> sharedLibraries,
                                Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies,
                                BundleDescriptor bundleDescriptor,
                                Map<BundleDescriptor, List<BundleDependency>> pluginsBundleDependencies) {
    this.packages = ImmutableList.copyOf(packages);
    this.resources = ImmutableList.copyOf(resources);
    this.artifactCoordinates = artifactCoordinates;
    this.projectFolder = projectFolder;
    this.dependencies = ImmutableList.copyOf(dependencies);
    this.sharedLibraries = ImmutableSet.copyOf(sharedLibraries);
    this.additionalPluginDependencies = ImmutableMap.copyOf(additionalPluginDependencies);
    this.bundleDescriptor = bundleDescriptor;
    this.pluginsBundleDependencies = ImmutableMap.copyOf(pluginsBundleDependencies);
  }

  /**
   * These are the packages containing java classes in the modeled project.
   * <p>
   * This does not take into account the java packages of this project's dependencies.
   * 
   * @return the java packages of the project.
   */
  public List<String> getPackages() {
    return packages;
  }

  /**
   * These are the resources in the modeled project.
   * <p>
   * This does not take into account the resources of this project's dependencies.
   * 
   * @return the resources of the project.
   */
  public List<String> getResources() {
    return resources;
  }

  /**
   * This is the GAV of the modeled project.
   * 
   * @return the coordinates of the artifact for this project.
   */
  public ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

  public File getProjectFolder() {
    return projectFolder;
  }

  /**
   * This are the dependencies of the modeled project, regardless of the classifier.
   * 
   * @return the dependencies of the artifact for this project
   */
  public List<BundleDependency> getDependencies() {
    return dependencies;
  }

  /**
   * These are the descriptors of the dependencies of the modeled project that are visible to the plugins of this project.
   * <p>
   * Elements contained in this set must exist in the {@link BundleDependency#getDescriptor()} of the {@link #getDependencies()
   * dependencies}.
   * 
   * @return the shared libraries of the artifact for this project.
   */
  public Set<BundleDescriptor> getSharedLibraries() {
    return sharedLibraries;
  }

  /**
   * These are dependencies that are added for each plugins.
   * <p>
   * In each entry of this map, the dependencies in the value will be added to the plugin represented by the key.
   * <p>
   * Keys of this map must exist in the {@link BundleDependency#getDescriptor()} of the {@link #getDependencies() dependencies}.
   * 
   * @return the additional dependencies for the plugins of this project.
   */
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
