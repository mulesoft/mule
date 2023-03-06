/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.maven.pom.parser.api.model.Classifier.MULE_PLUGIN;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.BundleDependency;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.maven.pom.parser.api.model.AdditionalPluginDependencies;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.StringUtils;


/**
 * Resolves additional plugin libraries for all plugins declared.
 */
public class AdditionalPluginDependenciesResolver {

  protected static final String MULE_APPLICATION_CLASSIFIER = "mule-application";

  private final File temporaryFolder;
  private final Map<ArtifactCoordinates, Supplier<MavenPomModel>> pomModels;

  private final MavenClient mavenClient;

  private final List<AdditionalPluginDependencies> pluginsWithAdditionalDependencies;

  public AdditionalPluginDependenciesResolver(MavenClient muleMavenPluginClient,
                                              List<AdditionalPluginDependencies> additionalPluginDependencies,
                                              File temporaryFolder) {
    this(muleMavenPluginClient, additionalPluginDependencies, temporaryFolder, emptyMap());
  }

  public AdditionalPluginDependenciesResolver(MavenClient muleMavenPluginClient,
                                              List<AdditionalPluginDependencies> additionalPluginDependencies,
                                              File temporaryFolder,
                                              Map<ArtifactCoordinates, Supplier<MavenPomModel>> pomModels) {
    this.mavenClient = muleMavenPluginClient;
    this.pluginsWithAdditionalDependencies = new ArrayList<>(additionalPluginDependencies);
    this.temporaryFolder = temporaryFolder;
    this.pomModels = pomModels;
  }

  public Map<BundleDependency, List<BundleDependency>> resolveDependencies(List<BundleDependency> applicationDependencies,
                                                                           Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    addPluginDependenciesAdditionalLibraries(applicationDependencies);
    Map<BundleDependency, List<BundleDependency>> pluginsWithAdditionalDeps = new LinkedHashMap<>();
    for (AdditionalPluginDependencies pluginWithAdditionalDependencies : pluginsWithAdditionalDependencies) {
      BundleDependency pluginBundleDependency =
          getPluginBundleDependency(pluginWithAdditionalDependencies, applicationDependencies);
      List<Artifact> pluginDependencies =
          getPluginDependencies(pluginWithAdditionalDependencies, pluginsDependencies);
      List<BundleDependency> additionalDependencies =
          resolveDependencies(pluginWithAdditionalDependencies.getAdditionalDependencies().stream()
              .filter(additionalDep -> pluginDependencies.stream()
                  .noneMatch(artifactDependency -> areSameArtifact(additionalDep, artifactDependency)))
              .collect(toList()));
      if (!additionalDependencies.isEmpty()) {
        pluginsWithAdditionalDeps.put(pluginBundleDependency,
                                      additionalDependencies);
      }
    }
    return pluginsWithAdditionalDeps;
  }

  private List<BundleDependency> resolveDependencies(List<BundleDescriptor> additionalDependencies) {
    return mavenClient.resolveArtifactDependencies(additionalDependencies,
                                                   of(mavenClient.getMavenConfiguration()
                                                       .getLocalMavenRepositoryLocation()),
                                                   empty());
  }

  private BundleDependency getPluginBundleDependency(AdditionalPluginDependencies plugin,
                                                     List<BundleDependency> mulePlugins) {
    return mulePlugins.stream()
        .filter(mulePlugin -> StringUtils.equals(mulePlugin.getDescriptor().getArtifactId(), plugin.getArtifactId())
            && StringUtils.equals(mulePlugin.getDescriptor().getGroupId(), plugin.getGroupId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Declared additional dependencies for a plugin not present: "
            + plugin)));
  }

  private List<Artifact> getPluginDependencies(AdditionalPluginDependencies plugin,
                                               Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    return pluginsDependencies.entrySet().stream().filter(
                                                          pluginDependenciesEntry -> StringUtils
                                                              .equals(pluginDependenciesEntry.getKey().getGroupId(),
                                                                      plugin.getGroupId())
                                                              && StringUtils.equals(pluginDependenciesEntry.getKey()
                                                                  .getArtifactId(), plugin.getArtifactId()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find dependencies for plugin: "
            + plugin)));
  }

  private boolean areSameArtifact(BundleDescriptor dependency, Artifact artifact) {
    return StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactCoordinates().getArtifactId())
        && StringUtils.equals(dependency.getGroupId(), artifact.getArtifactCoordinates().getGroupId())
        && StringUtils.equals(dependency.getVersion(), artifact.getArtifactCoordinates().getVersion());
  }

  private void addPluginDependenciesAdditionalLibraries(List<BundleDependency> applicationDependencies) {
    List<BundleDependency> mulePlugins = applicationDependencies
        .stream()
        .filter(bundleDependency -> MULE_PLUGIN
            .equals(bundleDependency.getDescriptor().getClassifier().orElse(null)))
        .collect(toList());

    Collection<AdditionalPluginDependencies> additionalDependenciesFromMulePlugins =
        resolveAdditionalDependenciesFromMulePlugins(mulePlugins);

    pluginsWithAdditionalDependencies.addAll(additionalDependenciesFromMulePlugins.stream()
        .filter(isNotRedefinedAtApplicationLevel())
        .collect(toList()));
  }

  private Collection<AdditionalPluginDependencies> resolveAdditionalDependenciesFromMulePlugins(List<BundleDependency> mulePlugins) {
    Map<String, AdditionalPluginDependencies> additionalDependenciesFromMulePlugins = new HashMap<>();

    // See LightweightDeployableProjectModelBuilderTestCase#createDeployableProjectModelWithAdditionalDependenciesInAPlugin
    mulePlugins.stream()
        .filter(mulePlugin -> {
          Supplier<MavenPomModel> modelSupplier = pomModels.get(getArtifactCoordinates(mulePlugin));
          if (modelSupplier != null) {
            return modelSupplier.get().getPackaging().equals(MULE_APPLICATION_CLASSIFIER);
          }
          return mavenClient.getRawPomModel(new File(mulePlugin.getBundleUri())).getPackaging()
              .equals(MULE_APPLICATION_CLASSIFIER);
        })
        .forEach(mulePlugin -> {

          Supplier<MavenPomModel> pomModel =
              pomModels.getOrDefault(getArtifactCoordinates(mulePlugin),
                                     () -> mavenClient.getEffectiveModel(new File(mulePlugin.getBundleUri()),
                                                                         of(temporaryFolder)));

          MavenPomParser mavenPomParser =
              discoverProvider().createMavenPomParserClient(pomModel.get().getPomFile().get().toPath());

          mavenPomParser.getPomAdditionalPluginDependenciesForArtifacts().values().forEach(mavenPlugin -> {
            String artifact = mavenPlugin.getGroupId() + ":" + mavenPlugin.getArtifactId();
            AdditionalPluginDependencies alreadyDefinedPluginAdditionalDependencies =
                additionalDependenciesFromMulePlugins.get(artifact);
            if (alreadyDefinedPluginAdditionalDependencies != null) {
              LinkedList<BundleDescriptor> effectiveDependencies =
                  new LinkedList<>(alreadyDefinedPluginAdditionalDependencies.getAdditionalDependencies());
              mavenPlugin.getAdditionalDependencies().forEach(additionalDependenciesDependency -> {
                boolean addDependency = true;
                for (int i = 0; i < effectiveDependencies.size(); i++) {
                  BundleDescriptor effectiveDependency = effectiveDependencies.get(i);
                  if (effectiveDependency.getGroupId().equals(additionalDependenciesDependency.getGroupId()) &&
                      effectiveDependency.getArtifactId().equals(additionalDependenciesDependency.getArtifactId())
                      &&
                      effectiveDependency.getType().equals(additionalDependenciesDependency.getType()) &&
                      effectiveDependency.getClassifier()
                          .equals(additionalDependenciesDependency.getClassifier())) {
                    if (isNewerVersion(additionalDependenciesDependency.getVersion(),
                                       effectiveDependency.getVersion())) {
                      effectiveDependencies.remove(i);
                    } else {
                      addDependency = false;
                    }
                    break;
                  }
                }
                if (addDependency) {
                  effectiveDependencies.add(additionalDependenciesDependency);
                }
              });
              AdditionalPluginDependencies alreadyDefinedEffectivePluginAdditionalDependencies =
                  new AdditionalPluginDependencies(alreadyDefinedPluginAdditionalDependencies, effectiveDependencies);
              additionalDependenciesFromMulePlugins.replace(artifact, alreadyDefinedEffectivePluginAdditionalDependencies);
            } else {
              additionalDependenciesFromMulePlugins.put(mavenPlugin.getGroupId() + ":" + mavenPlugin.getArtifactId(),
                                                        mavenPlugin);
            }
          });
        });


    return additionalDependenciesFromMulePlugins.values();
  }

  private boolean isNewerVersion(String dependencyA, String dependencyB) {
    try {
      return new Semver(dependencyA, LOOSE).isGreaterThan(new Semver(dependencyB, LOOSE));
    } catch (IllegalArgumentException e) {
      // If not using semver lets just compare the strings.
      return dependencyA.compareTo(dependencyB) > 0;
    }
  }

  private ArtifactCoordinates getArtifactCoordinates(BundleDependency mulePlugin) {
    BundleDescriptor descriptor = mulePlugin.getDescriptor();
    return new ArtifactCoordinates(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion());
  }

  private Predicate<AdditionalPluginDependencies> isNotRedefinedAtApplicationLevel() {
    return dependencyPluginAdditionalDependencies -> !pluginsWithAdditionalDependencies.stream()
        .filter(applicationPluginAdditionalDependency -> (dependencyPluginAdditionalDependencies.getGroupId()
            .equals(applicationPluginAdditionalDependency.getGroupId())
            && dependencyPluginAdditionalDependencies.getArtifactId()
                .equals(applicationPluginAdditionalDependency.getArtifactId())))
        .findAny().isPresent();
  }
}
