/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.validateMuleRuntimeSharedLibrary;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;

import java.io.File;
import java.util.List;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * {@link ClassLoaderModel.ClassLoaderModelBuilder ClassLoaderModelBuilder} that adds the concept of Shared Library for the
 * configured dependencies.
 */
public class ArtifactClassLoaderConfigurationBuilder extends ClassLoaderModel.ClassLoaderModelBuilder {

  private static final Logger LOGGER = getLogger(ArtifactClassLoaderConfigurationBuilder.class);

  private final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;
  private final File artifactFolder;

  public ArtifactClassLoaderConfigurationBuilder(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                 File artifactFolder) {
    this.packagerClassLoaderModel = packagerClassLoaderModel;
    this.artifactFolder = artifactFolder;
  }

  @Override
  public ClassLoaderModel build() {
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
        .filter(sharedDep -> !validateMuleRuntimeSharedLibrary(sharedDep.getArtifactCoordinates().getGroupId(),
                                                               sharedDep.getArtifactCoordinates().getArtifactId()))
        .forEach(sharedDep -> {
          this.exportingPackages(sharedDep.getPackages() == null ? emptySet() : Sets.newHashSet(sharedDep.getPackages()));
          this.exportingResources(sharedDep.getResources() == null ? emptySet()
              : Sets.newHashSet(sharedDep.getResources()));
        });
  }

  protected void processAdditionalPluginLibraries() {
    if (packagerClassLoaderModel instanceof AppClassLoaderModel) {
      AppClassLoaderModel appClassLoaderModel = (AppClassLoaderModel) packagerClassLoaderModel;
      appClassLoaderModel.getAdditionalPluginDependencies()
          .ifPresent(additionalDeps -> additionalDeps.forEach(this::updateDependency));
    }
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

  protected void replaceBundleDependency(BundleDependency original, BundleDependency modified) {
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
        .setPackages(artifact.getPackages() == null ? emptySet() : Sets.newHashSet(artifact.getPackages()));
    bundleDependencyBuilder
        .setResources(artifact.getResources() == null ? emptySet() : Sets.newHashSet(artifact.getResources()));
    return bundleDependencyBuilder.build();
  }

}
