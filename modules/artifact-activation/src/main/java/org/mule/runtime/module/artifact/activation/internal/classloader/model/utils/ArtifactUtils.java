/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model.utils;

import static org.mule.runtime.module.artifact.activation.internal.classloader.Classifier.MULE_DOMAIN;
import static org.mule.runtime.module.artifact.activation.internal.classloader.Classifier.MULE_PLUGIN;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.tools.api.classloader.model.ApplicationGAVModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper methods to convert artifact related classes and recognize mule plugin artifacts.
 */
public class ArtifactUtils {

  private static final String PACKAGE_TYPE = "jar";
  private static final String PROVIDED = "provided";
  private static final URI EMPTY_RESOURCE = URI.create("");
  private static final String POM_TYPE = "pom";

  /**
   * Convert a {@link BundleDescriptor} instance to {@link ArtifactCoordinates}.
   *
   * @param bundleDescriptor the bundle descriptor to be converted.
   * @return the corresponding artifact coordinates with normalized version.
   */
  public static ArtifactCoordinates toArtifactCoordinates(BundleDescriptor bundleDescriptor) {
    return new ArtifactCoordinates(bundleDescriptor.getGroupId(), bundleDescriptor.getArtifactId(),
                                   bundleDescriptor.getBaseVersion(),
                                   bundleDescriptor.getType(), bundleDescriptor.getClassifier().orElse(null));
  }

  public static ArtifactCoordinates toArtifactCoordinates(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor bundleDescriptor) {
    return new ArtifactCoordinates(bundleDescriptor.getGroupId(),
                                   bundleDescriptor.getArtifactId(),
                                   bundleDescriptor.getBaseVersion(),
                                   bundleDescriptor.getType(),
                                   bundleDescriptor.getClassifier().orElse(null));
  }

  /**
   * Convert a {@link org.mule.maven.pom.parser.api.model.BundleDependency} instance to {@link Artifact}.
   *
   * @param bundleDependency the bundle dependency to be converted.
   * @return the corresponding artifact with normalized version.
   */
  public static Artifact toArtifact(org.mule.maven.pom.parser.api.model.BundleDependency bundleDependency) {
    ArtifactCoordinates artifactCoordinates = toArtifactCoordinates(bundleDependency.getDescriptor());
    return new Artifact(artifactCoordinates, bundleDependency.getBundleUri());
  }

  /**
   * Convert a {@link org.mule.maven.pom.parser.api.model.BundleDependency} instance to {@link Artifact}.
   *
   * @param bundleDependency the bundle dependency to be converted.
   * @return the corresponding artifact with normalized version.
   */
  public static Artifact toArtifact(BundleDependency bundleDependency) {
    ArtifactCoordinates artifactCoordinates = toArtifactCoordinates(bundleDependency.getDescriptor());
    return new Artifact(artifactCoordinates, bundleDependency.getBundleUri());
  }

  /**
   * Converts a {@link List<BundleDependency>} to a {@link List<Artifact>}.
   *
   * @param dependencies the bundle dependency list to be converted.
   * @return the corresponding artifact list, each one with normalized version.
   */
  public static List<Artifact> toArtifacts(List<BundleDependency> dependencies) {
    return dependencies.stream().map(ArtifactUtils::toArtifact).collect(toList());
  }

  public static List<Artifact> toApplicationModelArtifacts(List<BundleDependency> appDependencies) {
    List<Artifact> dependencies = toArtifacts(appDependencies);
    dependencies.forEach(ArtifactUtils::updateScopeIfDomain);
    return dependencies;
  }

  public static List<Artifact> updatePackagesResources(List<Artifact> artifacts) {
    return artifacts.stream().map(ArtifactUtils::updatePackagesResources).collect(toList());
  }

  public static Artifact updatePackagesResources(Artifact artifact) {
    if (MULE_PLUGIN.equals(artifact.getArtifactCoordinates().getClassifier())
        || artifact.getUri() == null
        // mule-domain are set with a "" URI
        || isBlank(artifact.getUri().getPath())) {
      return artifact;
    }
    JarInfo jarInfo = new FileJarExplorer(false).explore(artifact.getUri());
    artifact.setPackages(jarInfo.getPackages().toArray(new String[0]));
    artifact.setResources(jarInfo.getResources().toArray(new String[0]));
    return artifact;
  }

  public static List<Artifact> findArtifactsSharedDependencies(List<BundleDependency> appDependencies, List<Artifact> artifacts,
                                                               MavenPomParser parser, List<String> activeProfiles) {
    return parser.getSharedLibraries()
        .stream()
        .flatMap(shareLibrary -> findAndExportSharedLibraries(shareLibrary.getGroupId(),
                                                              shareLibrary.getArtifactId(),
                                                              artifacts,
                                                              appDependencies))
        .collect(toList());
  }

  private static Stream<Artifact> findAndExportSharedLibraries(String sharedLibraryGroupId,
                                                               String sharedLibraryArtifactId,
                                                               List<Artifact> artifacts,
                                                               List<BundleDependency> deployableDependencies) {
    return deployableDependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getGroupId().equals(sharedLibraryGroupId) &&
            bundleDependency.getDescriptor().getArtifactId().equals(sharedLibraryArtifactId))
        .flatMap(sharedBundleDependency -> filterTransitiveSharedDependencies(artifacts, sharedBundleDependency));
  }

  private static Stream<Artifact> filterTransitiveSharedDependencies(List<Artifact> artifacts,
                                                                     BundleDependency sharedBundleDependency) {
    return concat(filterSharedArtifacts(sharedBundleDependency.getDescriptor().getGroupId(),
                                        sharedBundleDependency.getDescriptor().getArtifactId(),
                                        artifacts),
                  sharedBundleDependency.getTransitiveDependenciesList()
                      .stream()
                      .flatMap(transitiveDependency -> filterTransitiveSharedDependencies(artifacts, transitiveDependency)));
  }

  private static Stream<Artifact> filterSharedArtifacts(String sharedLibraryGroupId,
                                                        String sharedLibraryArtifactId,
                                                        List<Artifact> artifacts) {
    return artifacts
        .stream()
        .filter(artifact -> artifact.getArtifactCoordinates().getGroupId().equals(sharedLibraryGroupId) &&
            artifact.getArtifactCoordinates().getArtifactId().equals(sharedLibraryArtifactId));
  }

  public static void updateScopeIfDomain(Artifact artifact) {
    String classifier = artifact.getArtifactCoordinates().getClassifier();
    if (StringUtils.equals(classifier, MULE_DOMAIN.toString())) {
      artifact.getArtifactCoordinates().setScope(PROVIDED);
      artifact.setUri(EMPTY_RESOURCE);
    }
  }

  public static ArtifactCoordinates getDeployableArtifactCoordinates(MavenPomParser parser, ApplicationGAVModel appGAVModel) {
    ArtifactCoordinates deployableCoordinates = toArtifactCoordinates(getPomProjectBundleDescriptor(appGAVModel));
    deployableCoordinates.setType(PACKAGE_TYPE);
    deployableCoordinates.setClassifier(parser.getModel().getPackaging());
    return deployableCoordinates;
  }

  public static BundleDescriptor getPomProjectBundleDescriptor(ApplicationGAVModel appGAVModel) {
    return getBundleDescriptor(appGAVModel);
  }


  public static BundleDescriptor getBundleDescriptor(ApplicationGAVModel appGAVModel) {
    return new BundleDescriptor.Builder()
        .setGroupId(appGAVModel.getGroupId())
        .setArtifactId(appGAVModel.getArtifactId())
        .setVersion(appGAVModel.getVersion())
        .setBaseVersion(appGAVModel.getVersion())
        .setType(POM_TYPE)
        .build();
  }

}
