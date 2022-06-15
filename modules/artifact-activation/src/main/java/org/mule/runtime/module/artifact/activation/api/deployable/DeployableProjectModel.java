/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
  private final BundleDescriptor descriptor;
  private final Supplier<MuleDeployableModel> deployableModelSupplier;
  private final File projectFolder;
  private final List<BundleDependency> dependencies;
  private final List<BundleDependency> muleRuntimeDependencies;
  private final Set<BundleDescriptor> sharedLibraries;
  private final Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies;

  /**
   * Creates a new instance with the provided parameters.
   * <p>
   * Performs a validation of consistency of the provided parameters. If any validation fails, an {@link IllegalArgumentException}
   * is thrown indication the situation that caused it.
   *
   * @param packages                     See {@link #getPackages()}
   * @param resources                    See {@link #getResources()}
   * @param descriptor                   See {@link #getDescriptor()}
   * @param deployableModelSupplier      See {@link #getDeployableModel()}
   * @param projectFolder                See {@link #getProjectFolder()}
   * @param dependencies                 See {@link #getDependencies()}
   * @param muleRuntimeDependencies      See {@link #getMuleRuntimeDependencies()}
   * @param sharedLibraries              See {@link #getSharedLibraries()}
   * @param additionalPluginDependencies See {@link #additionalPluginDependencies}
   * @throws IllegalArgumentException if there are consistency problems with the provided parameters.
   */
  public DeployableProjectModel(List<String> packages,
                                List<String> resources,
                                BundleDescriptor descriptor,
                                Supplier<MuleDeployableModel> deployableModelSupplier,
                                File projectFolder,
                                List<BundleDependency> dependencies,
                                List<BundleDependency> muleRuntimeDependencies,
                                Set<BundleDescriptor> sharedLibraries,
                                Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies)
      throws IllegalArgumentException {
    List<String> validationMessages = new ArrayList<>();

    for (BundleDescriptor sharedLibDescriptor : sharedLibraries) {
      if (dependencies.stream()
          .noneMatch(dep -> dep.getDescriptor().equals(sharedLibDescriptor))) {
        validationMessages.add(format("Artifact '%s' is declared as a sharedLibrary but is not a dependency of the project",
                                      sharedLibDescriptor.getGroupId() + ":" + sharedLibDescriptor.getArtifactId()));
      }
    }

    for (BundleDescriptor pluginDescriptor : additionalPluginDependencies.keySet()) {
      if (dependencies.stream()
          .noneMatch(dep -> dep.getDescriptor().equals(pluginDescriptor))) {
        validationMessages
            .add(format("Mule Plugin '%s' is declared in additionalPluginDependencies but is not a dependency of the project",
                        pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId()));
      }
    }

    // TODO W-11202204 review this
    // new MulePluginsCompatibilityValidator()
    // .validate(dependencies.stream()
    // .map(BundleDependency::getDescriptor)
    // .filter(dependencyDescriptor -> MULE_PLUGIN_CLASSIFIER
    // .equals(dependencyDescriptor.getClassifier().orElse(null)))
    // .collect(toList()))
    // .entrySet()
    // .forEach(result -> validationMessages
    // .add(format("Mule Plugin '%s' is depended upon in the project with incompatible versions ('%s') in the dependency graph.",
    // result.getKey(),
    // result.getValue().stream().map(BundleDescriptor::getVersion).collect(joining(", ")))));

    if (!validationMessages.isEmpty()) {
      throw new IllegalArgumentException(validationMessages.stream()
          .collect(joining(" * ", lineSeparator() + " * ", lineSeparator())));
    }

    this.packages = ImmutableList.copyOf(packages);
    this.resources = ImmutableList.copyOf(resources);
    this.descriptor = requireNonNull(descriptor);
    this.deployableModelSupplier = new LazyValue<>(requireNonNull(deployableModelSupplier));
    this.projectFolder = requireNonNull(projectFolder);
    this.dependencies = ImmutableList.copyOf(dependencies);
    this.muleRuntimeDependencies = muleRuntimeDependencies;
    this.sharedLibraries = ImmutableSet.copyOf(sharedLibraries);
    this.additionalPluginDependencies = ImmutableMap.copyOf(additionalPluginDependencies);
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
   * This contains the GAV of the modeled project.
   * 
   * @return the descriptor of the artifact for this project.
   */
  public BundleDescriptor getDescriptor() {
    return descriptor;
  }

  /**
   * Mule projects contain additional model information within the project itself (i.e.: mule-artifact.json file). Calling this
   * getter will trigger the loading of that additional data from within the project.
   * 
   * @return the additional model information from this project.
   */
  public MuleDeployableModel getDeployableModel() {
    return deployableModelSupplier.get();
  }

  /**
   * This folder will be used to create the classloader for the deployable project.
   * <p>
   * Temporary files related to this project will also be created within this directory.
   * 
   * @return the folder where this deployable project is located.
   */
  public File getProjectFolder() {
    return projectFolder;
  }

  /**
   * These are the dependencies of the modeled project, regardless of the classifier.
   *
   * @return the dependencies of the artifact for this project
   */
  public List<BundleDependency> getDependencies() {
    return dependencies;
  }

  /**
   * These are the dependencies of the modeled project that will be provided by the environment, except for domains.
   *
   * @return the dependencies of the artifact for this project that will be provided by the environment.
   */
  public List<BundleDependency> getMuleRuntimeDependencies() {
    return muleRuntimeDependencies;
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
   * These are dependencies that are added for each plugin.
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
}
