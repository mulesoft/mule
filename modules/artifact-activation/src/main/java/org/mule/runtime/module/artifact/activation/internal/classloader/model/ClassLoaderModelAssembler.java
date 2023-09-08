/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.util.List;
import java.util.Set;

/**
 * Assembles the class loader model for an artifact given all its pieces.
 */
public class ClassLoaderModelAssembler {

  protected static final String CLASS_LOADER_MODEL_VERSION = "1.2.0";

  private final ArtifactCoordinates artifactCoordinates;
  private final List<BundleDependency> projectDependencies;
  private final Set<BundleDescriptor> sharedProjectDependencies;
  private final List<String> availablePackages;
  private final List<String> availableResources;

  public ClassLoaderModelAssembler(ArtifactCoordinates artifactCoordinates,
                                   List<BundleDependency> projectDependencies,
                                   Set<BundleDescriptor> sharedProjectDependencies,
                                   List<String> availablePackages,
                                   List<String> availableResources) {
    this.artifactCoordinates = requireNonNull(artifactCoordinates);
    this.projectDependencies = requireNonNull(projectDependencies);
    this.sharedProjectDependencies = requireNonNull(sharedProjectDependencies);
    this.availablePackages = requireNonNull(availablePackages);
    this.availableResources = requireNonNull(availableResources);
  }

  public ClassLoaderModel createClassLoaderModel() {
    ClassLoaderModel classLoaderModel = new ClassLoaderModel(CLASS_LOADER_MODEL_VERSION, getArtifactCoordinates());

    assembleClassLoaderModel(classLoaderModel);

    return classLoaderModel;
  }

  protected final void assembleClassLoaderModel(ClassLoaderModel classLoaderModel) {
    classLoaderModel.setDependencies(toArtifacts(projectDependencies));
    classLoaderModel.setPackages(availablePackages.toArray(new String[0]));
    classLoaderModel.setResources(availableResources.toArray(new String[0]));
  }

  /**
   * Convert a {@link BundleDependency} instance to {@link Artifact}.
   *
   * @param bundleDependency the bundle dependency to be converted.
   * @return the artifact for the provided dependency.
   */
  private Artifact toArtifact(BundleDependency bundleDependency) {
    ArtifactCoordinates artifactCoordinates = toArtifactCoordinates(bundleDependency.getDescriptor());
    Artifact artifact = new Artifact(artifactCoordinates, bundleDependency.getBundleUri());
    artifact.setPackages(bundleDependency.getPackages() == null ? new String[0]
        : bundleDependency.getPackages().toArray(new String[0]));
    artifact.setResources(bundleDependency.getResources() == null ? new String[0]
        : bundleDependency.getResources().toArray(new String[0]));
    if (sharedProjectDependencies.contains(bundleDependency.getDescriptor())) {
      artifact.setShared(true);
    }

    return artifact;
  }

  /**
   * Converts a {@link List<BundleDependency>} to a {@link List<Artifact>}.
   *
   * @param dependencies the bundle dependency list to be converted.
   * @return the corresponding artifact list.
   */
  protected List<Artifact> toArtifacts(List<BundleDependency> dependencies) {
    return dependencies.stream().map(this::toArtifact).collect(toList());
  }

  /**
   * Convert a {@link BundleDescriptor} instance to {@link ArtifactCoordinates}.
   *
   * @param bundleDescriptor the bundle descriptor to be converted.
   * @return the corresponding artifact coordinates.
   */
  private ArtifactCoordinates toArtifactCoordinates(BundleDescriptor bundleDescriptor) {
    return new ArtifactCoordinates(bundleDescriptor.getGroupId(), bundleDescriptor.getArtifactId(),
                                   bundleDescriptor.getBaseVersion(),
                                   bundleDescriptor.getType(), bundleDescriptor.getClassifier().orElse(null));
  }

  protected ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

  protected List<BundleDependency> getProjectDependencies() {
    return projectDependencies;
  }
}
