/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import com.google.common.collect.ImmutableSet;
import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
  private Set<BundleDependency> nonProvidedDependencies;
  private File temporaryFolder;
  private Map<Pair<String, String>, Boolean> sharedLibraryAlreadyExported = new HashMap<>();

  public LightweightClassLoaderModelBuilder(File artifactFolder, BundleDescriptor artifactBundleDescriptor,
                                            MavenClient mavenClient, Set<BundleDependency> nonProvidedDependencies) {
    super(artifactFolder, artifactBundleDescriptor);
    this.mavenClient = mavenClient;
    this.nonProvidedDependencies = nonProvidedDependencies;
    File localMavenRepositoryLocation = mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation();
    this.temporaryFolder = new File(localMavenRepositoryLocation, ".mule");
  }

  @Override
  protected void replaceBundleDependency(BundleDependency original, BundleDependency modified) {
    super.replaceBundleDependency(original, processPluginAdditionalDependenciesURIs(modified));
  }

  private BundleDependency processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    return new BundleDependency.Builder(bundleDependency)
        .setAdditionalDependencies(resolveDependencies(bundleDependency.getAdditionalDependencies()))
        .build();
  }

  // TODO: MULE-15768
  // TODO: MULE-16026 all the following logic is duplicated from the mule-packager.
  private Set<BundleDependency> resolveDependencies(Set<BundleDependency> additionalDependencies) {
    List<BundleDependency> resolvedAdditionalDependencies = new ArrayList<>();
    additionalDependencies.stream()
        .map(BundleDependency::getDescriptor)
        .map(descriptor -> new org.mule.maven.client.api.model.BundleDescriptor.Builder()
            .setGroupId(descriptor.getGroupId())
            .setArtifactId(descriptor.getArtifactId())
            .setVersion(descriptor.getVersion())
            .setType(descriptor.getType())
            .setClassifier(descriptor.getClassifier().orElse(null)).build())
        .forEach(bundleDescriptor -> {
          resolveDependency(bundleDescriptor)
              .forEach(additionalDep -> updateAdditionalDependencyOrFail(resolvedAdditionalDependencies, additionalDep));
        });
    return ImmutableSet.copyOf(resolvedAdditionalDependencies);
  }

  private List<BundleDependency> resolveDependency(org.mule.maven.client.api.model.BundleDescriptor mavenDependencyBundleDescriptor) {
    List<BundleDependency> resolvedDependencies = new ArrayList<>();
    resolvedDependencies.add(toBundleDescriptor(mavenClient.resolveBundleDescriptor(mavenDependencyBundleDescriptor)));
    resolvedDependencies
        .addAll(mavenClient.resolveBundleDescriptorDependencies(false, false, mavenDependencyBundleDescriptor).stream()
            .map(mavenBundleDependency -> toBundleDescriptor(mavenBundleDependency)).collect(toList()));
    return resolvedDependencies;
  }

  private BundleDependency toBundleDescriptor(org.mule.maven.client.api.model.BundleDependency resolveBundleDescriptor) {
    return new BundleDependency.Builder()
        .setDescriptor(new BundleDescriptor.Builder()
            .setGroupId(resolveBundleDescriptor.getDescriptor().getGroupId())
            .setArtifactId(resolveBundleDescriptor.getDescriptor().getArtifactId())
            .setBaseVersion(resolveBundleDescriptor.getDescriptor().getBaseVersion())
            .setVersion(resolveBundleDescriptor.getDescriptor().getVersion())
            .setType(resolveBundleDescriptor.getDescriptor().getType())
            .setClassifier(resolveBundleDescriptor.getDescriptor().getClassifier().orElse(null))
            .build())
        .setBundleUri(resolveBundleDescriptor.getBundleUri())
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
    resolvedBundleDependency.getTransitiveDependencies()
        .forEach(this::exportBundleDependencyAndTransitiveDependencies);
  }

  private void updateAdditionalDependencyOrFail(List<BundleDependency> additionalDependencies,
                                                BundleDependency bundleDependency) {
    Reference<BundleDependency> replace = new Reference<>();
    additionalDependencies.stream()
        .filter(additionalBundleDependency -> StringUtils.equals(additionalBundleDependency.getDescriptor().getGroupId(),
                                                                 bundleDependency.getDescriptor().getGroupId())
            &&
            StringUtils.equals(additionalBundleDependency.getDescriptor().getArtifactId(),
                               bundleDependency.getDescriptor().getArtifactId()))
        .findFirst()
        .map(additionalBundleDependency -> {
          String additionalBundleDependencyVersion = additionalBundleDependency.getDescriptor().getVersion();
          String bundleDependencyVersion = bundleDependency.getDescriptor().getVersion();
          if (areSameMajor(bundleDependencyVersion, additionalBundleDependencyVersion)) {
            if (isNewerVersion(bundleDependencyVersion, additionalBundleDependencyVersion)) {
              replace.set(additionalBundleDependency);
            }
          } else {
            throw new MuleRuntimeException(createStaticMessage("Attempting to add different major versions of the same dependency as additional plugin dependency. If this is not explicitly defined, check transitive dependencies."
                +
                lineSeparator() +
                "These are: " +
                lineSeparator() +
                additionalBundleDependency.toString() +
                lineSeparator() +
                bundleDependency.toString()));
          }
          return true;
        }).orElseGet(
                     () -> additionalDependencies.add(bundleDependency));
    if (replace.get() != null) {
      additionalDependencies.remove(replace.get());
      additionalDependencies.add(bundleDependency);
    }
  }

  private boolean isNewerVersion(String dependencyA, String dependencyB) {
    try {
      return new Semver(dependencyA, LOOSE).isGreaterThan(new Semver(dependencyB, LOOSE));
    } catch (IllegalArgumentException e) {
      // If not using semver lets just compare the strings.
      return dependencyA.compareTo(dependencyB) > 0;
    }
  }

  private boolean areSameMajor(String dependencyA, String dependencyB) {
    try {
      return new Semver(dependencyA, LOOSE).getMajor().equals(new Semver(dependencyB, LOOSE).getMajor());
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  protected Map<BundleDescriptor, Set<BundleDescriptor>> doProcessAdditionalPluginLibraries(Plugin packagingPlugin) {
    if (!temporaryFolder.exists() && !temporaryFolder.mkdirs()) {
      throw new MuleRuntimeException(createStaticMessage("Could not create temporary folder under "
          + temporaryFolder.getAbsolutePath()));
    }

    Map<BundleDescriptor, Set<BundleDescriptor>> deployableArtifactAdditionalLibrariesMap =
        super.doProcessAdditionalPluginLibraries(packagingPlugin);
    Map<BundleDescriptor, Set<BundleDescriptor>> effectivePluginsAdditionalLibrariesMap =
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
            Map<BundleDescriptor, Set<BundleDescriptor>> pluginAdditionalLibrariesMap =
                super.doProcessAdditionalPluginLibraries(artifactPackagerPlugin.get());
            pluginAdditionalLibrariesMap.forEach((pluginDescriptor, additionalLibraries) -> {
              Set<BundleDescriptor> effectivePluginAdditionalLibraries = new HashSet<>();
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
                    .collect(Collectors.toSet()));
              } else {
                effectivePluginAdditionalLibraries.addAll(additionalLibraries);
              }
              effectivePluginsAdditionalLibrariesMap.put(pluginDescriptor, additionalLibraries);
            });
          });
        });
    return effectivePluginsAdditionalLibrariesMap;
  }

  private boolean existsInLibrariesMap(Map<BundleDescriptor, Set<BundleDescriptor>> additionalLibrariesPerPluginMap,
                                       BundleDescriptor plugin, BundleDescriptor additionalLibrary) {
    Set<BundleDescriptor> additionalLibraries = additionalLibrariesPerPluginMap.get(plugin);
    if (additionalLibraries == null) {
      return false;
    }
    return additionalLibraries.contains(additionalLibrary);
  }

  private Optional<BundleDescriptor> findLibraryInAdditionalLibrariesMap(Map<BundleDescriptor, Set<BundleDescriptor>> additionalLibrariesPerPluginMap,
                                                                         BundleDescriptor plugin,
                                                                         BundleDescriptor additionalLibrary) {
    Set<BundleDescriptor> additionalLibraries = additionalLibrariesPerPluginMap.get(plugin);
    if (additionalLibraries == null) {
      return empty();
    }
    return additionalLibraries.stream().filter(bundleDescriptor -> bundleDescriptor.equals(additionalLibrary)).findAny();
  }

}
