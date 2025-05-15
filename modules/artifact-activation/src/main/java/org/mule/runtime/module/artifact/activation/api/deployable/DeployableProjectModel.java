/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactConstants.getApiClassifiers;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
  private final Optional<MuleProjectStructure> projectStructure;
  private final File projectFolder;
  private final List<BundleDependency> dependencies;
  private final Set<BundleDescriptor> sharedLibraries;
  private final Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies;

  private final List<Path> resourcesPath;

  /**
   * Creates a new instance with the provided parameters.
   *
   * @param packages                     See {@link #getPackages()}
   * @param resources                    See {@link #getResources()}
   * @param resourcesPath                See {@link #getResourcesPath()}
   * @param descriptor                   See {@link #getDescriptor()}
   * @param deployableModelSupplier      See {@link #getDeployableModel()}
   * @param projectFolder                See {@link #getProjectFolder()}
   * @param dependencies                 See {@link #getDependencies()}
   * @param sharedLibraries              See {@link #getSharedLibraries()}
   * @param additionalPluginDependencies See {@link #additionalPluginDependencies}
   */

  public DeployableProjectModel(List<String> packages,
                                List<String> resources,
                                List<Path> resourcesPath,
                                BundleDescriptor descriptor,
                                Supplier<MuleDeployableModel> deployableModelSupplier,
                                File projectFolder,
                                List<BundleDependency> dependencies,
                                Set<BundleDescriptor> sharedLibraries,
                                Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies) {
    this(packages,
         resources,
         resourcesPath,
         descriptor,
         deployableModelSupplier,
         empty(),
         projectFolder,
         dependencies,
         sharedLibraries,
         additionalPluginDependencies);
  }

  /**
   * Creates a new instance with the provided parameters.
   *
   * @param packages                     See {@link #getPackages()}
   * @param resources                    See {@link #getResources()}
   * @param resourcesPath                See {@link #getResourcesPath()}
   * @param descriptor                   See {@link #getDescriptor()}
   * @param deployableModelSupplier      See {@link #getDeployableModel()}
   * @param projectStructure             See {@link #getProjectStructure()}
   * @param projectFolder                See {@link #getProjectFolder()}
   * @param dependencies                 See {@link #getDependencies()}
   * @param sharedLibraries              See {@link #getSharedLibraries()}
   * @param additionalPluginDependencies See {@link #additionalPluginDependencies}
   */

  public DeployableProjectModel(List<String> packages,
                                List<String> resources,
                                List<Path> resourcesPath,
                                BundleDescriptor descriptor,
                                Supplier<MuleDeployableModel> deployableModelSupplier,
                                Optional<MuleProjectStructure> projectStructure,
                                File projectFolder,
                                List<BundleDependency> dependencies,
                                Set<BundleDescriptor> sharedLibraries,
                                Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies) {

    this.packages = unmodifiableList(new ArrayList<>(packages));
    this.resources = unmodifiableList(new ArrayList<>(resources));
    this.resourcesPath = unmodifiableList(new ArrayList<>(resourcesPath));
    this.descriptor = requireNonNull(descriptor);
    this.deployableModelSupplier = new LazyValue<>(requireNonNull(deployableModelSupplier));
    this.projectStructure = requireNonNull(projectStructure);
    this.projectFolder = requireNonNull(projectFolder);
    this.dependencies = unmodifiableList(new ArrayList<>(dependencies));
    this.sharedLibraries = unmodifiableSet(new HashSet<>(sharedLibraries));
    this.additionalPluginDependencies = unmodifiableMap(new HashMap<>(additionalPluginDependencies));
  }

  /**
   * Performs a validation of consistency of the model fields. If any validation fails, an {@link ArtifactActivationException} is
   * thrown indicating the situation that caused it.
   *
   * @throws ArtifactActivationException if there are consistency problems with the model fields.
   */
  public void validate() throws ArtifactActivationException {
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

    getRepeatedDependencies(dependencies).forEach((key, value) -> validationMessages
        .add(format("Mule Plugin '%s' is depended upon in the project multiple times with versions ('%s') in the dependency graph.",
                    key, value.stream().map(BundleDescriptor::getVersion).collect(joining(", ")))));

    if (!validationMessages.isEmpty()) {
      throw new ArtifactActivationException(createStaticMessage(validationMessages.stream()
          .collect(joining(" * ", lineSeparator() + " * ", lineSeparator()))));
    }
  }

  private Map<String, List<BundleDescriptor>> getRepeatedDependencies(List<BundleDependency> dependencies) {
    Map<String, List<BundleDescriptor>> repeatedDependencies = new HashMap<>();

    for (BundleDependency dependency : dependencies) {
      BundleDescriptor descriptor = dependency.getDescriptor();
      if (descriptor.getClassifier().map(classifier -> !getApiClassifiers().contains(classifier)).orElse(true)) {
        String pluginKey = descriptor.getGroupId() + ":" + descriptor.getArtifactId()
            + descriptor.getClassifier().map(classifier -> ":" + classifier).orElse("");
        repeatedDependencies.computeIfAbsent(pluginKey, k -> new ArrayList<>());
        repeatedDependencies.get(pluginKey).add(descriptor);
      }
    }

    return repeatedDependencies.entrySet().stream().filter(entry -> entry.getValue().size() > 1)
        .collect(toMap(Entry::getKey, Entry::getValue));
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
   * These are the resources in the modeled project. These resources are located in one of the folders of {@code resourcesPath}.
   * <p>
   * This does not take into account the resources of this project's dependencies.
   *
   *
   * @return the resources of the project.
   */
  public List<String> getResources() {
    return resources;
  }

  /**
   * These are the paths to the resources folders in the modeled project.
   *
   * @return the resources of the project.
   */
  public List<Path> getResourcesPath() {
    return resourcesPath;
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
   * This structure will be used to obtain the sources/resources and create the classloader for the deployable project.
   * <p>
   * Temporary files related to this project will also be created within the {@link MuleProjectStructure#getProjectFolder()}.
   *
   * @return the folder structure for this deployable project.
   *
   * @since 4.8
   */
  public Optional<MuleProjectStructure> getProjectStructure() {
    return projectStructure;
  }

  /**
   * This folder will be used to create the classloader for the deployable project.
   * <p>
   * Temporary files related to this project will also be created within this directory.
   *
   * @return the folder where this deployable project is located.
   */
  public File getProjectFolder() {
    return projectStructure
        .map(s -> s.getProjectFolder().toFile())
        .orElse(projectFolder);
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

}
