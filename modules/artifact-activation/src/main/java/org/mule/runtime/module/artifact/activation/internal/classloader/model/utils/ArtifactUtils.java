/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model.utils;

import static org.mule.runtime.module.artifact.activation.internal.classloader.Classifier.MULE_DOMAIN;
import static org.mule.runtime.module.artifact.activation.internal.classloader.Classifier.MULE_PLUGIN;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.PROVIDED;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper methods to convert artifact related classes and recognize mule plugin artifacts.
 */
public class ArtifactUtils {

  private static final URI EMPTY_RESOURCE = URI.create("");

  public static List<BundleDependency> toApplicationModelArtifacts(List<BundleDependency> appDependencies) {
    return appDependencies
        .stream()
        .map(ArtifactUtils::updateScopeIfDomain)
        .collect(toList());
  }

  private static BundleDependency updateScopeIfDomain(BundleDependency artifact) {
    if (artifact.getDescriptor().getClassifier().map(MULE_DOMAIN.toString()::equals).orElse(false)) {
      return BundleDependency.builder(artifact)
          .setScope(PROVIDED)
          .setBundleUri(EMPTY_RESOURCE)
          .build();
    } else {
      return artifact;
    }
  }

  public static List<BundleDependency> updatePackagesResources(List<BundleDependency> artifacts) {
    return artifacts
        .stream()
        .map(ArtifactUtils::updatePackagesResources)
        .collect(toList());
  }

  private static BundleDependency updatePackagesResources(BundleDependency artifact) {
    if (artifact.getDescriptor().getClassifier().map(MULE_PLUGIN.toString()::equals).orElse(false)
        || artifact.getBundleUri() == null
        // mule-domain are set with a "" URI
        || isBlank(artifact.getBundleUri().getPath())) {
      return artifact;
    }
    JarInfo jarInfo = new FileJarExplorer(false).explore(artifact.getBundleUri());
    return BundleDependency.builder(artifact)
        .setPackages(jarInfo.getPackages())
        .setResources(jarInfo.getResources())
        .build();

  }

  public static List<BundleDependency> findArtifactsSharedDependencies(List<BundleDependency> appDependencies,
                                                                       List<String> sharedLibrariesCoordinates,
                                                                       List<String> activeProfiles) {
    return sharedLibrariesCoordinates
        .stream()
        .flatMap(shareLibrary -> findAndExportSharedLibraries(shareLibrary,
                                                              appDependencies))
        .collect(toList());
  }

  private static Stream<BundleDependency> findAndExportSharedLibraries(String sharedLibraryCoordinates,
                                                                       List<BundleDependency> deployableDependencies) {
    return deployableDependencies.stream()
        .filter(bundleDependency -> sharedLibraryCoordinates
            .equals(bundleDependency.getDescriptor().getGroupId() + ":" + bundleDependency.getDescriptor().getArtifactId()))
        .flatMap(sharedBundleDependency -> filterTransitiveSharedDependencies(deployableDependencies, sharedBundleDependency));
  }

  private static Stream<BundleDependency> filterTransitiveSharedDependencies(List<BundleDependency> deployableDependencies,
                                                                             BundleDependency sharedBundleDependency) {
    return concat(filterSharedArtifacts(sharedBundleDependency.getDescriptor().getGroupId(),
                                        sharedBundleDependency.getDescriptor().getArtifactId(),
                                        deployableDependencies),
                  sharedBundleDependency.getTransitiveDependenciesList()
                      .stream()
                      .flatMap(transitiveDependency -> filterTransitiveSharedDependencies(deployableDependencies,
                                                                                          transitiveDependency)));
  }

  private static Stream<BundleDependency> filterSharedArtifacts(String sharedLibraryGroupId,
                                                                String sharedLibraryArtifactId,
                                                                List<BundleDependency> deployableDependencies) {
    return deployableDependencies
        .stream()
        .filter(artifact -> artifact.getDescriptor().getGroupId().equals(sharedLibraryGroupId) &&
            artifact.getDescriptor().getArtifactId().equals(sharedLibraryArtifactId));
  }

  public static org.mule.runtime.api.artifact.ArtifactCoordinates getDeployableArtifactCoordinates(String groupId,
                                                                                                   String artifactId,
                                                                                                   String version,
                                                                                                   String packaging) {
    return new BundleDescriptor.Builder()
        .setGroupId(groupId)
        .setArtifactId(artifactId)
        .setVersion(version)
        .setBaseVersion(version)
        .setClassifier(packaging)
        .build();
  }

}
