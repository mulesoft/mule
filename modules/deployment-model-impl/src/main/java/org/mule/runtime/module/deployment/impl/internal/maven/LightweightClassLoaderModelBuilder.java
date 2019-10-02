/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

/**
 * Builder for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} responsible of resolving dependencies
 * when light weight packaging is used for an artifact.
 *
 * @since 4.2.0
 */
public class LightweightClassLoaderModelBuilder extends ArtifactClassLoaderModelBuilder {

  private MavenClient mavenClient;
  private List<BundleDependency> nonProvidedDependencies;
  private File temporaryFolder;
  private Map<Pair<String, String>, Boolean> sharedLibraryAlreadyExported = new HashMap<>();

  public LightweightClassLoaderModelBuilder(File artifactFolder, BundleDescriptor artifactBundleDescriptor,
                                            MavenClient mavenClient, List<BundleDependency> nonProvidedDependencies) {
    super(artifactFolder, artifactBundleDescriptor);
    this.mavenClient = mavenClient;
    this.nonProvidedDependencies = nonProvidedDependencies;
    File localMavenRepositoryLocation = mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation();
    this.temporaryFolder = new File(localMavenRepositoryLocation, ".mule");
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    List<org.mule.maven.client.api.model.BundleDependency> resolvedAdditionalDependencies =
        mavenClient.resolveArtifactDependencies(
                                                bundleDependency.getAdditionalDependenciesList().stream()
                                                    .map(additionalDependency -> toMavenClientBundleDescriptor(additionalDependency
                                                        .getDescriptor()))
                                                    .collect(toList()),
                                                of(mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation()),
                                                empty());
    return resolvedAdditionalDependencies.stream()
        .map(org.mule.maven.client.api.model.BundleDependency::getBundleUri)
        .collect(toList());
  }

  private static org.mule.maven.client.api.model.BundleDescriptor toMavenClientBundleDescriptor(BundleDescriptor descriptor) {
    return new org.mule.maven.client.api.model.BundleDescriptor.Builder()
        .setGroupId(descriptor.getGroupId())
        .setArtifactId(descriptor.getArtifactId())
        .setVersion(descriptor.getVersion())
        .setType(descriptor.getType())
        .setClassifier(descriptor.getClassifier().orElse(null))
        .build();
  }

  @Override
  protected void findAndExportSharedLibrary(String groupId, String artifactId) {
    Pair<String, String> sharedLibraryKey = new Pair<>(groupId, artifactId);
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
    resolvedBundleDependency.getTransitiveDependenciesList()
        .forEach(this::exportBundleDependencyAndTransitiveDependencies);
  }

  @Override
  protected Map<BundleDescriptor, List<BundleDescriptor>> doProcessAdditionalPluginLibraries(Plugin packagingPlugin) {
    if (!temporaryFolder.exists() && !temporaryFolder.mkdirs()) {
      throw new MuleRuntimeException(createStaticMessage("Could not create temporary folder under "
          + temporaryFolder.getAbsolutePath()));
    }

    Map<BundleDescriptor, List<BundleDescriptor>> deployableArtifactAdditionalLibrariesMap =
        super.doProcessAdditionalPluginLibraries(packagingPlugin);
    Map<BundleDescriptor, List<BundleDescriptor>> effectivePluginsAdditionalLibrariesMap =
        new HashMap<>(deployableArtifactAdditionalLibrariesMap);
    nonProvidedDependencies.stream()
        .filter(bundleDependency -> MULE_PLUGIN.equals(bundleDependency.getDescriptor().getClassifier().orElse(null)))
        .forEach(bundleDependency -> {
          Model effectiveModel;
          try {
            effectiveModel = mavenClient.getEffectiveModel(toFile(bundleDependency.getBundleUri().toURL()), of(temporaryFolder));
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
          Optional<Plugin> artifactPackagerPlugin = findArtifactPackagerPlugin(effectiveModel);
          artifactPackagerPlugin.ifPresent(plugin -> {
            Map<BundleDescriptor, List<BundleDescriptor>> pluginAdditionalLibrariesMap =
                super.doProcessAdditionalPluginLibraries(artifactPackagerPlugin.get());
            pluginAdditionalLibrariesMap.forEach((pluginDescriptor, additionalLibraries) -> {
              List<BundleDescriptor> effectivePluginAdditionalLibraries = new ArrayList<>();
              if (deployableArtifactAdditionalLibrariesMap.containsKey(pluginDescriptor)) {
                effectivePluginAdditionalLibraries.addAll(additionalLibraries.stream()
                    .filter(additionalLibrary -> {
                      boolean additionalLibraryDefinedAtDeployableArtifact =
                          existsInLibrariesMap(deployableArtifactAdditionalLibrariesMap, pluginDescriptor, additionalLibrary);
                      if (!additionalLibraryDefinedAtDeployableArtifact) {
                        Optional<BundleDescriptor> additionalLibraryDefinedByAnotherPlugin =
                            findLibraryInAdditionalLibrariesMap(effectivePluginsAdditionalLibrariesMap, pluginDescriptor,
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
                    })
                    .collect(toList()));
              } else {
                effectivePluginAdditionalLibraries.addAll(additionalLibraries);
              }
              effectivePluginsAdditionalLibrariesMap.put(pluginDescriptor, additionalLibraries);
            });
          });
        });
    return effectivePluginsAdditionalLibrariesMap;
  }

  private boolean existsInLibrariesMap(Map<BundleDescriptor, List<BundleDescriptor>> additionalLibrariesPerPluginMap,
                                       BundleDescriptor plugin, BundleDescriptor additionalLibrary) {
    List<BundleDescriptor> additionalLibraries = additionalLibrariesPerPluginMap.get(plugin);
    if (additionalLibraries == null) {
      return false;
    }
    return additionalLibraries.contains(additionalLibrary);
  }

  private Optional<BundleDescriptor> findLibraryInAdditionalLibrariesMap(Map<BundleDescriptor, List<BundleDescriptor>> additionalLibrariesPerPluginMap,
                                                                         BundleDescriptor plugin,
                                                                         BundleDescriptor additionalLibrary) {
    List<BundleDescriptor> additionalLibraries = additionalLibrariesPerPluginMap.get(plugin);
    if (additionalLibraries == null) {
      return empty();
    }
    return additionalLibraries.stream().filter(bundleDescriptor -> bundleDescriptor.equals(additionalLibrary)).findAny();
  }

}
