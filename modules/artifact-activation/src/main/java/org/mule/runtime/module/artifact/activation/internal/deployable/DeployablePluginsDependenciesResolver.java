/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toArtifactCoordinates;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.toArtifacts;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils.updatePackagesResources;
import static org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.VersionUtils.getMajor;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import org.apache.commons.lang3.StringUtils;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves the dependencies of a deployable as {@link Artifact artifacts}.
 *
 * @since 4.5
 */
public class DeployablePluginsDependenciesResolver {

  public final Map<ArtifactCoordinates, List<Artifact>> resolve(List<BundleDependency> deployableDependencies) {
    Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies = new HashMap<>();

    List<BundleDependency> dependencies = deployableDependencies.stream()
        .filter(dep -> dep.getDescriptor().getClassifier().isPresent())
        .filter(dep -> dep.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER))
        .collect(Collectors.toList());

    Map<BundleDependency, List<BundleDependency>> dependenciesMap = resolveDependencies(dependencies);

    for (Map.Entry<BundleDependency, List<BundleDependency>> dependencyListEntry : dependenciesMap.entrySet()) {
      List<BundleDependency> dependencyDependencies =
          resolveConflicts(dependencyListEntry.getValue(), dependencies);
      pluginsDependencies.put(toArtifactCoordinates(dependencyListEntry.getKey().getDescriptor()),
                              updatePackagesResources(toArtifacts(dependencyDependencies)));
    }

    return pluginsDependencies;
  }

  private List<BundleDependency> resolveConflicts(List<BundleDependency> newDependencies,
                                                  List<BundleDependency> alreadyResolved) {
    return resolveMulePluginsVersions(newDependencies, alreadyResolved);
  }

  /**
   * Resolve each of the mule plugins dependencies.
   *
   * @param mulePlugins the list of mule plugins that are going to have their dependencies resolved.
   */
  public Map<BundleDependency, List<BundleDependency>> resolveDependencies(List<BundleDependency> mulePlugins) {
    Map<BundleDependency, List<BundleDependency>> muleDependenciesDependencies = new LinkedHashMap<>();
    for (BundleDependency muleDependency : mulePlugins) {
      muleDependenciesDependencies.put(muleDependency, collectTransitiveDependencies(muleDependency));
    }
    return muleDependenciesDependencies;
  }

  private List<BundleDependency> collectTransitiveDependencies(BundleDependency rootDependency) {
    List<BundleDependency> allTransitiveDependencies = new LinkedList<>();
    for (BundleDependency transitiveDependency : rootDependency.getTransitiveDependencies()) {
      allTransitiveDependencies.add(transitiveDependency);
      if (transitiveDependency.getDescriptor().getClassifier().map(c -> !MULE_PLUGIN_CLASSIFIER.equals(c)).orElse(true)) {
        allTransitiveDependencies.addAll(collectTransitiveDependencies(transitiveDependency));
      }
    }
    return allTransitiveDependencies;
  }

  protected List<BundleDependency> resolveMulePluginsVersions(List<BundleDependency> mulePluginsToResolve,
                                                              List<BundleDependency> definitiveMulePlugins) {
    List<BundleDependency> resolvedPlugins = new ArrayList<>();
    checkArgument(mulePluginsToResolve != null, "List of mule plugins to resolve should not be null");
    checkArgument(definitiveMulePlugins != null, "List of definitive mule plugins should not be null");

    for (BundleDependency mulePluginToResolve : mulePluginsToResolve) {
      Optional<BundleDependency> mulePlugin =
          definitiveMulePlugins.stream().filter(p -> hasSameArtifactIdAndMajor(p, mulePluginToResolve)).findFirst();
      resolvedPlugins.add(mulePlugin.orElse(mulePluginToResolve));
    }
    return resolvedPlugins;
  }

  protected boolean hasSameArtifactIdAndMajor(BundleDependency bundleDependency, BundleDependency otherBundleDependency) {
    BundleDescriptor descriptor = bundleDependency.getDescriptor();
    BundleDescriptor otherDescriptor = otherBundleDependency.getDescriptor();
    return StringUtils.equals(descriptor.getArtifactId(), otherDescriptor.getArtifactId())
        && StringUtils.equals(getMajor(descriptor.getBaseVersion()), getMajor(otherDescriptor.getBaseVersion()));
  }

}

