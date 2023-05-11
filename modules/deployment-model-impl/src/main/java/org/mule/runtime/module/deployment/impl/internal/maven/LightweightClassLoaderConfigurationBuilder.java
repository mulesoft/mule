/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.io.FileUtils.toFile;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.AdditionalPluginDependencies;
import org.mule.maven.pom.parser.api.model.ArtifactCoordinates;
import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Builder for a {@link ClassLoaderConfiguration} responsible for resolving dependencies when lightweight packaging is used for an
 * artifact.
 *
 * @since 4.2.0
 */
public class LightweightClassLoaderConfigurationBuilder extends ArtifactClassLoaderConfigurationBuilder {

  private final MavenClient mavenClient;
  private final List<BundleDependency> nonProvidedDependencies;
  private final Map<Pair<String, String>, Boolean> sharedLibraryAlreadyExported = new HashMap<>();

  public LightweightClassLoaderConfigurationBuilder(File artifactFolder, BundleDescriptor artifactBundleDescriptor,
                                                    MavenClient mavenClient, List<BundleDependency> nonProvidedDependencies) {
    super(artifactFolder, artifactBundleDescriptor);
    this.mavenClient = mavenClient;
    this.nonProvidedDependencies = nonProvidedDependencies;
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    List<org.mule.maven.pom.parser.api.model.BundleDependency> resolvedAdditionalDependencies =
        mavenClient.resolveArtifactDependencies(
                                                bundleDependency.getAdditionalDependenciesList().stream()
                                                    .map(additionalDependency -> toMavenClientBundleDescriptor(additionalDependency
                                                        .getDescriptor()))
                                                    .collect(toList()),
                                                of(mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation()),
                                                empty());
    return resolvedAdditionalDependencies.stream()
        .map(org.mule.maven.pom.parser.api.model.BundleDependency::getBundleUri)
        .collect(toList());
  }

  private static org.mule.maven.pom.parser.api.model.BundleDescriptor toMavenClientBundleDescriptor(BundleDescriptor descriptor) {
    return new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
        .setGroupId(descriptor.getGroupId())
        .setArtifactId(descriptor.getArtifactId())
        .setVersion(descriptor.getVersion())
        .setType(descriptor.getType())
        .setClassifier(descriptor.getClassifier().orElse(null))
        .build();
  }

  @Override
  protected void findAndExportSharedLibrary(String groupId, String artifactId) {
    Pair<String, String> sharedLibraryKey = Pair.of(groupId, artifactId);
    if (sharedLibraryAlreadyExported.containsKey(sharedLibraryKey)) {
      return;
    }
    sharedLibraryAlreadyExported.put(sharedLibraryKey, true);
    Optional<BundleDependency> matchingLibrary = this.nonProvidedDependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getGroupId().equals(groupId) &&
            bundleDependency.getDescriptor().getArtifactId().equals(artifactId))
        .findAny();
    BundleDependency bundleDependency = matchingLibrary.orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format(
                                                                                                                              "Dependency %s:%s could not be found within the artifact %s. It must be declared within the maven dependencies of the artifact.",
                                                                                                                              groupId,
                                                                                                                              artifactId,
                                                                                                                              artifactFolder
                                                                                                                                  .getName()))));

    exportBundleDependencyAndTransitiveDependencies(bundleDependency);
  }

  private void exportBundleDependencyAndTransitiveDependencies(final BundleDependency bundleDependency) {
    BundleDependency resolvedBundleDependency = bundleDependency;
    if (bundleDependency.getBundleUri() == null) {
      resolvedBundleDependency = this.nonProvidedDependencies.stream()
          .filter(nonProvidedDependency -> nonProvidedDependency.getDescriptor().getGroupId()
              .equals(bundleDependency.getDescriptor().getGroupId()) &&
              nonProvidedDependency.getDescriptor().getArtifactId().equals(bundleDependency.getDescriptor().getArtifactId()))
          .findAny()
          .orElse(bundleDependency);
    }
    JarInfo jarInfo = fileJarExplorer.explore(resolvedBundleDependency.getBundleUri());
    this.exportingPackages(jarInfo.getPackages());
    this.exportingResources(jarInfo.getResources());

    jarInfo.getServices()
        .forEach(service -> this
            .exportingResources(singleton("META-INF/services/" + service.getServiceInterface())));

    resolvedBundleDependency.getTransitiveDependenciesList()
        .forEach(this::exportBundleDependencyAndTransitiveDependencies);
  }

  @Override
  protected Map<ArtifactCoordinates, AdditionalPluginDependencies> doProcessAdditionalPluginLibraries(MavenPomParser parser) {
    Map<ArtifactCoordinates, AdditionalPluginDependencies> deployableArtifactAdditionalLibrariesMap =
        super.doProcessAdditionalPluginLibraries(parser);
    Map<ArtifactCoordinates, AdditionalPluginDependencies> effectivePluginsAdditionalLibrariesMap =
        new HashMap<>(deployableArtifactAdditionalLibrariesMap);
    nonProvidedDependencies.stream()
        .filter(bundleDependency -> MULE_PLUGIN.equals(bundleDependency.getDescriptor().getClassifier().orElse(null)))
        .forEach(bundleDependency -> {
          MavenPomModel effectiveModel;

          try {
            effectiveModel = mavenClient.getEffectiveModel(toFile(bundleDependency.getBundleUri().toURL()), empty());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }

          if (effectiveModel.getPomFile().isPresent()) {
            MavenPomParser parserForPlugin = discoverProvider()
                .createMavenPomParserClient(effectiveModel.getPomFile().get().toPath(), getActiveProfiles());
            Map<ArtifactCoordinates, AdditionalPluginDependencies> pomAdditionalPluginDependenciesForArtifact =
                parserForPlugin.getPomAdditionalPluginDependenciesForArtifacts();
            pomAdditionalPluginDependenciesForArtifact.forEach((artifact, additionalDependenciesForArtifact) -> {
              if (deployableArtifactAdditionalLibrariesMap.containsKey(artifact)) {
                AdditionalPluginDependencies additionalPluginDependenciesForPlugin =
                    deployableArtifactAdditionalLibrariesMap.get(artifact);
                List<org.mule.maven.pom.parser.api.model.BundleDescriptor> effectiveAdditionalDependencies =
                    additionalDependenciesForArtifact.getAdditionalDependencies().stream()
                        .filter(additionalLibrary -> {
                          boolean additionalLibraryDefinedAtDeployableArtifact =
                              existsInLibrariesMap(deployableArtifactAdditionalLibrariesMap,
                                                   artifact, additionalLibrary);
                          if (!additionalLibraryDefinedAtDeployableArtifact) {
                            Optional<org.mule.maven.pom.parser.api.model.BundleDescriptor> additionalLibraryDefinedByAnotherPlugin =
                                findLibraryInAdditionalLibrariesMap(effectivePluginsAdditionalLibrariesMap,
                                                                    artifact,
                                                                    additionalLibrary);
                            try {
                              return !additionalLibraryDefinedByAnotherPlugin.isPresent()
                                  || new MuleVersion(additionalLibrary.getVersion())
                                      .newerThan(additionalLibraryDefinedByAnotherPlugin.get().getVersion());
                            } catch (IllegalStateException e) {
                              // If not using semver lets just compare the strings.
                              return additionalLibrary.getVersion()
                                  .compareTo(additionalLibraryDefinedByAnotherPlugin.get().getVersion()) > 0;
                            }
                          }
                          // Let's use the one defined and the main artifact since it may be overriding the declared by the plugin
                          return false;
                        }).collect(toCollection(LinkedList::new));
                AdditionalPluginDependencies effectiveAdditionalPluginDependenciesForPlugin =
                    new AdditionalPluginDependencies(additionalPluginDependenciesForPlugin, effectiveAdditionalDependencies);
                deployableArtifactAdditionalLibrariesMap.replace(artifact, effectiveAdditionalPluginDependenciesForPlugin);
              } else {
                effectivePluginsAdditionalLibrariesMap.put(artifact, additionalDependenciesForArtifact);
              }
            });
          }
        });
    return effectivePluginsAdditionalLibrariesMap;
  }

  private boolean existsInLibrariesMap(Map<ArtifactCoordinates, AdditionalPluginDependencies> additionalLibrariesPerPluginMap,
                                       ArtifactCoordinates plugin,
                                       org.mule.maven.pom.parser.api.model.BundleDescriptor additionalLibrary) {
    List<org.mule.maven.pom.parser.api.model.BundleDescriptor> additionalLibraries =
        additionalLibrariesPerPluginMap.get(plugin).getAdditionalDependencies();
    if (additionalLibraries == null) {
      return false;
    }
    return additionalLibraries.contains(additionalLibrary);
  }

  private Optional<org.mule.maven.pom.parser.api.model.BundleDescriptor> findLibraryInAdditionalLibrariesMap(Map<ArtifactCoordinates, AdditionalPluginDependencies> additionalLibrariesPerPluginMap,
                                                                                                             ArtifactCoordinates plugin,
                                                                                                             org.mule.maven.pom.parser.api.model.BundleDescriptor additionalLibrary) {
    List<org.mule.maven.pom.parser.api.model.BundleDescriptor> additionalLibraries =
        additionalLibrariesPerPluginMap.get(plugin).getAdditionalDependencies();
    if (additionalLibraries == null) {
      return empty();
    }
    return additionalLibraries.stream().filter(bundleDescriptor -> bundleDescriptor.equals(additionalLibrary)).findAny();
  }

}
