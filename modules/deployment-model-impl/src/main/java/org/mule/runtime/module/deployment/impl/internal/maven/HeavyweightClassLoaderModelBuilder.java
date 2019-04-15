/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static java.util.Collections.emptyMap;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;

import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;

/**
 * Builder for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} with information from a
 * {@link org.mule.tools.api.classloader.model.ClassLoaderModel} included when packaging the artifact in a heavyweight manner.
 *
 * @since 4.2.0
 */
public class HeavyweightClassLoaderModelBuilder extends ArtifactClassLoaderModelBuilder {

  private static final Semver CLASS_LOADER_MODEL_VERSION_110 = new Semver("1.1.0", LOOSE);

  private org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;

  public HeavyweightClassLoaderModelBuilder(File applicationFolder, BundleDescriptor artifactBundleDescriptor,
                                            org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel) {
    super(applicationFolder, artifactBundleDescriptor);
    this.packagerClassLoaderModel = packagerClassLoaderModel;
  }

  /**
   * Exports the shared libraries resources and packages.
   */
  @Override
  protected void doExportSharedLibrariesResourcesAndPackages(Plugin packagingPlugin) {
    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_110)) {
      super.doExportSharedLibrariesResourcesAndPackages(packagingPlugin);
    } else {
      exportSharedLibrariesResourcesAndPackages();
    }
  }

  @Override
  protected Map<BundleDescriptor, Set<BundleDescriptor>> doProcessAdditionalPluginLibraries(Plugin packagingPlugin) {
    if (packagerClassLoaderModel instanceof AppClassLoaderModel) {
      AppClassLoaderModel appClassLoaderModel = (AppClassLoaderModel) packagerClassLoaderModel;
      appClassLoaderModel.getAdditionalPluginDependencies()
          .ifPresent(additionalDeps -> additionalDeps.forEach(this::updateDependency));
    }
    return emptyMap();
  }

  private BundleDependency createExtendedBundleDependency(BundleDependency original,
                                                          Set<BundleDependency> additionalPluginDependencies) {
    return new BundleDependency.Builder(original).setAdditionalDependencies(additionalPluginDependencies).build();
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
                                                                                                  .collect(Collectors.toSet()))));
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
    return builder
        .setBundleUri(new File(artifactFolder, artifact.getUri().toString()).toURI())
        .setDescriptor(new BundleDescriptor.Builder()
            .setArtifactId(artifact.getArtifactCoordinates().getArtifactId())
            .setGroupId(artifact.getArtifactCoordinates().getGroupId())
            .setVersion(artifact.getArtifactCoordinates().getVersion())
            .setClassifier(artifact.getArtifactCoordinates().getClassifier())
            .setType(artifact.getArtifactCoordinates().getType())
            .build())
        .build();
  }

  /**
   * Exports shared libraries resources and packages getting the information from the packager
   * {@link org.mule.tools.api.classloader.model.ClassLoaderModel}.
   */
  private void exportSharedLibrariesResourcesAndPackages() {
    packagerClassLoaderModel.getDependencies().stream()
        .filter(Artifact::isShared)
        .forEach(
                 sharedDep -> findAndExportSharedLibrary(
                                                         sharedDep.getArtifactCoordinates().getGroupId(),
                                                         sharedDep.getArtifactCoordinates().getArtifactId()));
  }
}
