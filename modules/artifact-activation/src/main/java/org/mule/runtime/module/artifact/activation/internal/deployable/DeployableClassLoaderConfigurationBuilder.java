/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Sets.newHashSet;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link ClassLoaderConfigurationBuilder ClassLoaderConfigurationBuilder} that adds the concept of Shared Library for the configured dependencies.
 */
public class DeployableClassLoaderConfigurationBuilder extends ClassLoaderConfigurationBuilder {

  private final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;
  private final File artifactFolder;

  public DeployableClassLoaderConfigurationBuilder(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                   File artifactFolder) {
    if (!(packagerClassLoaderModel instanceof AppClassLoaderModel)) {
      throw new IllegalArgumentException("Class loader model must be an 'AppClassLoaderModel' for deployables.");
    }

    this.packagerClassLoaderModel = packagerClassLoaderModel;
    this.artifactFolder = artifactFolder;
  }

  @Override
  public ClassLoaderConfiguration build() {
    exportSharedLibrariesResourcesAndPackages();
    processAdditionalPluginLibraries();

    return super.build();
  }

  /**
   * Exports shared libraries resources and packages getting the information from the packager
   * {@link org.mule.tools.api.classloader.model.ClassLoaderModel}.
   */
  private void exportSharedLibrariesResourcesAndPackages() {
    packagerClassLoaderModel.getDependencies().stream()
        .filter(Artifact::isShared)
        // No need to validate the shared dependency here, as it has already been done by now
        .forEach(sharedDep -> {
          this.exportingPackages(sharedDep.getPackages() == null ? emptySet() : newHashSet(sharedDep.getPackages()));
          this.exportingResources(sharedDep.getResources() == null ? emptySet()
              : newHashSet(sharedDep.getResources()));
        });
  }

  private void processAdditionalPluginLibraries() {
    AppClassLoaderModel appClassLoaderModel = (AppClassLoaderModel) packagerClassLoaderModel;
    appClassLoaderModel.getAdditionalPluginDependencies()
        .ifPresent(additionalDeps -> additionalDeps.forEach(this::updateDependency));
  }

  private void updateDependency(org.mule.tools.api.classloader.model.Plugin plugin) {
    dependencies.stream()
        .filter(dep -> areSameDependency(plugin, dep))
        .findFirst()
        .ifPresent(
                   pluginDependency -> replaceBundleDependency(
                                                               pluginDependency,
                                                               createExtendedBundleDependency(
                                                                                              pluginDependency,
                                                                                              plugin.getAdditionalDependencies()
                                                                                                  .stream()
                                                                                                  .map(this::toBundleDependency)
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

  private boolean areSameDependency(org.mule.tools.api.classloader.model.Plugin plugin, BundleDependency dependency) {
    return StringUtils.equals(dependency.getDescriptor().getGroupId(), plugin.getGroupId())
        && StringUtils.equals(dependency.getDescriptor().getArtifactId(), plugin.getArtifactId());
  }

  private BundleDependency toBundleDependency(Artifact artifact) {
    BundleDependency.Builder builder = new BundleDependency.Builder();
    if (artifact.getArtifactCoordinates().getScope() != null) {
      builder.setScope(BundleScope.valueOf(artifact.getArtifactCoordinates().getScope().toUpperCase()));
    }

    BundleDependency.Builder bundleDependencyBuilder = builder
        .setBundleUri(artifact.getUri().isAbsolute()
            ? artifact.getUri()
            : new File(artifactFolder, artifact.getUri().toString()).toURI())
        .setDescriptor(new BundleDescriptor.Builder()
            .setArtifactId(artifact.getArtifactCoordinates().getArtifactId())
            .setGroupId(artifact.getArtifactCoordinates().getGroupId())
            .setVersion(artifact.getArtifactCoordinates().getVersion())
            .setClassifier(artifact.getArtifactCoordinates().getClassifier())
            .setType(artifact.getArtifactCoordinates().getType())
            .build());

    bundleDependencyBuilder
        .setPackages(artifact.getPackages() == null ? emptySet() : newHashSet(artifact.getPackages()));
    bundleDependencyBuilder
        .setResources(artifact.getResources() == null ? emptySet() : newHashSet(artifact.getResources()));
    return bundleDependencyBuilder.build();
  }

}
