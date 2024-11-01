/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

/**
 * {@link ClassLoaderConfigurationBuilder ClassLoaderConfigurationBuilder} that adds the concept of Shared Library for the
 * configured dependencies.
 */
public class DeployableClassLoaderConfigurationBuilder extends ClassLoaderConfigurationBuilder {

  private final DeployableProjectModel deployableProjectModel;
  // TODO remove this?
  private final File artifactFolder;

  public DeployableClassLoaderConfigurationBuilder(DeployableProjectModel deployableProjectModel,
                                                   File artifactFolder) {
    if (!(deployableProjectModel.getDescriptor().getClassifier()
        .map(c -> MULE_APPLICATION_CLASSIFIER.equals(c) || MULE_DOMAIN_CLASSIFIER.equals(c)).orElse(false))) {
      throw new IllegalArgumentException("Model must be for a '" + MULE_APPLICATION_CLASSIFIER + "' or '" + MULE_DOMAIN_CLASSIFIER
          + "' for deployables.");
    }

    this.deployableProjectModel = deployableProjectModel;
    this.artifactFolder = artifactFolder;
  }

  @Override
  public ClassLoaderConfiguration build() {
    exportSharedLibrariesResourcesAndPackages();
    processAdditionalPluginLibraries();

    return super.build();
  }

  /**
   * Exports shared libraries resources and packages getting the information from the {@link DeployableProjectModel}.
   */
  private void exportSharedLibrariesResourcesAndPackages() {
    deployableProjectModel.getDependencies()
        .stream()
        .filter(dep -> deployableProjectModel.getSharedLibraries().contains(dep.getDescriptor()))
        // No need to validate the shared dependency here, as it has already been done by now
        .forEach(sharedDep -> {
          this.exportingPackages(sharedDep.getPackages() == null ? emptySet()
              : new HashSet<>(sharedDep.getPackages()));
          this.exportingResources(sharedDep.getResources() == null ? emptySet()
              : new HashSet<>(sharedDep.getResources()));
        });
  }

  private void processAdditionalPluginLibraries() {
    deployableProjectModel.getAdditionalPluginDependencies().entrySet()
        .forEach(this::updateDependency);
  }

  private void updateDependency(Entry<BundleDescriptor, List<BundleDependency>> plugin) {
    dependencies.stream()
        .filter(pluginDependency -> plugin.getKey().equals(pluginDependency.getDescriptor()))
        .findFirst()
        .ifPresent(pluginDependency -> replaceBundleDependency(pluginDependency,
                                                               createExtendedBundleDependency(pluginDependency,
                                                                                              plugin.getValue()
                                                                                                  .stream()
                                                                                                  .collect(toList()))));
  }

  private BundleDependency createExtendedBundleDependency(BundleDependency original,
                                                          List<BundleDependency> additionalPluginDependencies) {
    return new BundleDependency.Builder(original).setAdditionalDependencies(additionalPluginDependencies).build();
  }

  private void replaceBundleDependency(BundleDependency original, BundleDependency modified) {
    this.dependencies.remove(original);
    this.dependencies.add(modified);
  }

}
