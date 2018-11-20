/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
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

  public LightweightClassLoaderModelBuilder(File artifactFolder,
                                            MavenClient mavenClient, Set<BundleDependency> nonProvidedDependencies,
                                            File temporaryFolder) {
    super(artifactFolder);
    this.mavenClient = mavenClient;
    this.nonProvidedDependencies = nonProvidedDependencies;
    this.temporaryFolder = temporaryFolder;
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    return resolveDependencies(bundleDependency.getAdditionalDependencies()).stream()
        .map(org.mule.maven.client.api.model.BundleDependency::getBundleUri).collect(toList());
  }

  // TODO: MULE-15768
  //TODO: MULE-16026 all the following logic is duplicated from the mule-packager.
  private List<org.mule.maven.client.api.model.BundleDependency> resolveDependencies(Set<BundleDependency> additionalDependencies) {
    List<org.mule.maven.client.api.model.BundleDependency> resolvedAdditionalDependencies = new ArrayList<>();
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
    return resolvedAdditionalDependencies;
  }

  private List<org.mule.maven.client.api.model.BundleDependency> resolveDependency(org.mule.maven.client.api.model.BundleDescriptor dependencyBundleDescriptor) {
    List<org.mule.maven.client.api.model.BundleDependency> resolvedDependencies = new ArrayList<>();
    resolvedDependencies.add(mavenClient.resolveBundleDescriptor(dependencyBundleDescriptor));
    resolvedDependencies.addAll(mavenClient.resolveBundleDescriptorDependencies(false, false, dependencyBundleDescriptor));
    return resolvedDependencies;
  }

  private void updateAdditionalDependencyOrFail(List<org.mule.maven.client.api.model.BundleDependency> additionalDependencies,
                                                org.mule.maven.client.api.model.BundleDependency bundleDependency) {
    Reference<org.mule.maven.client.api.model.BundleDependency> replace = new Reference<>();
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
    File temporaryPomFolder = new File(temporaryFolder, "temporary-poms");
    if (!temporaryPomFolder.exists() && !temporaryPomFolder.mkdirs()) {
      throw new MuleRuntimeException(createStaticMessage("Could not create temporary folder under "
          + temporaryPomFolder.getAbsolutePath()));
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
            effectiveModel =
                mavenClient.getEffectiveModel(toFile(bundleDependency.getBundleUri().toURL()), of(temporaryPomFolder));
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
