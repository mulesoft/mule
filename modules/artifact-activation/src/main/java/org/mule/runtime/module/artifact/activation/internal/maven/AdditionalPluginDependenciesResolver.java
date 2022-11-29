/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils;
import org.mule.runtime.module.artifact.activation.internal.plugin.Plugin;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;


/**
 * Resolves additional plugin libraries for all plugins declared.
 */
public class AdditionalPluginDependenciesResolver {

  private final AetherMavenClient aetherMavenClient;
  private final List<Plugin> pluginsWithAdditionalDependencies;

  public AdditionalPluginDependenciesResolver(AetherMavenClient muleMavenPluginClient,
                                              List<Plugin> additionalPluginDependencies) {
    this.aetherMavenClient = muleMavenPluginClient;
    this.pluginsWithAdditionalDependencies = new ArrayList<>(additionalPluginDependencies);
  }

  public Map<BundleDependency, List<BundleDependency>> resolveDependencies(List<BundleDependency> applicationDependencies,
                                                                           Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    Map<BundleDependency, List<BundleDependency>> pluginsWithAdditionalDeps = new LinkedHashMap<>();
    for (Plugin pluginWithAdditionalDependencies : pluginsWithAdditionalDependencies) {
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

  private List<BundleDependency> resolveDependencies(List<Dependency> additionalDependencies) {
    return aetherMavenClient.resolveArtifactDependencies(additionalDependencies.stream()
        .map(ArtifactUtils::toBundleDescriptor)
        .collect(toList()),
                                                         of(aetherMavenClient.getMavenConfiguration()
                                                             .getLocalMavenRepositoryLocation()),
                                                         empty());
  }

  private BundleDependency getPluginBundleDependency(Plugin plugin, List<BundleDependency> mulePlugins) {
    return mulePlugins.stream()
        .filter(mulePlugin -> StringUtils.equals(mulePlugin.getDescriptor().getArtifactId(), plugin.getArtifactId())
            && StringUtils.equals(mulePlugin.getDescriptor().getGroupId(), plugin.getGroupId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Declared additional dependencies for a plugin not present: "
            + plugin)));
  }

  private List<Artifact> getPluginDependencies(Plugin plugin, Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
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

  private boolean areSameArtifact(Dependency dependency, Artifact artifact) {
    return StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactCoordinates().getArtifactId())
        && StringUtils.equals(dependency.getGroupId(), artifact.getArtifactCoordinates().getGroupId())
        && StringUtils.equals(dependency.getVersion(), artifact.getArtifactCoordinates().getVersion());
  }
}
