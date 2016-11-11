/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;
import org.mule.runtime.deployment.model.api.artifact.DependencyNotFoundException;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves plugin dependencies considering the plugin name only.
 */
public class NamePluginDependenciesResolver implements PluginDependenciesResolver {

  private final ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory;
  private final DependenciesProvider dependenciesProvider;

  /**
   * Assembly the complete list of artifacts, while sorting them in a lexicographic order by name to then resolve sanitize
   * the exported packages and resource by the plugin's dependencies (avoids exporting elements that are already exported
   * by other plugin).
   *
   * @param artifactDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency to resolve
   * @param dependenciesProvider resolver for missing dependencies.
   */
  public NamePluginDependenciesResolver(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory,
                                        DependenciesProvider dependenciesProvider) {
    this.artifactDescriptorFactory = artifactDescriptorFactory;
    this.dependenciesProvider = dependenciesProvider;
  }

  @Override
  public List<ArtifactPluginDescriptor> resolve(List<ArtifactPluginDescriptor> descriptors) {

    Set<String> knownPlugins = descriptors.stream().map(ArtifactDescriptor::getName).collect(Collectors.toSet());
    descriptors = getArtifactsWithDependencies(descriptors, knownPlugins);


    descriptors.sort((d1, d2) -> (d1.getName().compareTo(d2.getName())));

    List<ArtifactPluginDescriptor> resolvedPlugins = new LinkedList<>();
    List<ArtifactPluginDescriptor> unresolvedPlugins = new LinkedList<>(descriptors);

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedPlugins.size();

      List<ArtifactPluginDescriptor> pendingUnresolvedPlugins = new LinkedList<>();

      for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
        if (isResolvedPlugin(unresolvedPlugin, resolvedPlugins)) {
          sanitizeExportedPackages(unresolvedPlugin, resolvedPlugins);
          resolvedPlugins.add(unresolvedPlugin);
        } else {
          pendingUnresolvedPlugins.add(unresolvedPlugin);
        }
      }

      // Will try to resolve the plugins that are still unresolved
      unresolvedPlugins = pendingUnresolvedPlugins;

      continueResolution = resolvedPlugins.size() > initialResolvedCount;
    }

    if (unresolvedPlugins.size() != 0) {
      throw new PluginResolutionError(createResolutionErrorMessage(unresolvedPlugins, resolvedPlugins));
    }

    return resolvedPlugins;
  }

  /**
   * Goes over the elements in the {@code pluginDescriptors} collection looking if it hasn't been resolved yet. If it hasn't
   * then it looks it up through the {@link DependenciesProvider} and then creates an {@link ArtifactPluginDescriptor}.
   *
   * @param pluginDescriptors plugins to validate.
   * @param visited plugins that are already resolved (by either the container or application initially, or by the resolver).
   * @return the plugins that were obtained initially plus all the ones that were found.
   * @throws DependencyNotFoundException if any dependency wasn't found properly
   */
  private List<ArtifactPluginDescriptor> getArtifactsWithDependencies(List<ArtifactPluginDescriptor> pluginDescriptors,
                                                                      Set<String> visited) {
    if (!pluginDescriptors.isEmpty()) {
      List<ArtifactPluginDescriptor> foundDependencies = new ArrayList<>();
      pluginDescriptors.stream()
          .filter(pluginDescriptor -> !pluginDescriptor.getClassLoaderModel().getDependencies().isEmpty())
          .forEach(pluginDescriptor -> pluginDescriptor.getClassLoaderModel().getDependencies()
              .forEach(dependency -> {
                if (!visited.contains(dependency)) {
                  File mulePluginLocation = dependenciesProvider.resolve(dependency);
                  foundDependencies.add(artifactDescriptorFactory.create(new File(mulePluginLocation.toURI())));
                  visited.add(dependency);
                }
              }));

      pluginDescriptors.addAll(getArtifactsWithDependencies(foundDependencies, visited));
    }
    return pluginDescriptors;
  }

  private void sanitizeExportedPackages(ArtifactPluginDescriptor pluginDescriptor,
                                        List<ArtifactPluginDescriptor> resolvedPlugins) {

    final Set<String> packagesExportedByDependencies =
        findDependencyPackageClosure(pluginDescriptor.getClassLoaderModel().getDependencies(), resolvedPlugins);

    ClassLoaderModel originalClassLoaderModel = pluginDescriptor.getClassLoaderModel();
    final Set<String> exportedClassPackages = new HashSet<>(originalClassLoaderModel.getExportedPackages());
    exportedClassPackages.removeAll(packagesExportedByDependencies);
    pluginDescriptor.setClassLoaderModel(new ClassLoaderModelBuilder(originalClassLoaderModel)
        .exportingPackages(exportedClassPackages).build());

  }

  private Set<String> findDependencyPackageClosure(Set<String> pluginDependencies,
                                                   List<ArtifactPluginDescriptor> resolvedPlugins) {
    Set<String> exportedPackages = new HashSet<>();
    for (String pluginDependency : pluginDependencies) {
      ArtifactPluginDescriptor dependencyDescriptor = findArtifactPluginDescriptor(pluginDependency, resolvedPlugins);
      exportedPackages.addAll(dependencyDescriptor.getClassLoaderModel().getExportedPackages());
      exportedPackages
          .addAll(findDependencyPackageClosure(dependencyDescriptor.getClassLoaderModel().getDependencies(), resolvedPlugins));
    }

    return exportedPackages;
  }

  protected static String createResolutionErrorMessage(List<ArtifactPluginDescriptor> unresolvedPlugins,
                                                       List<ArtifactPluginDescriptor> resolvedPlugins) {
    StringBuilder builder = new StringBuilder("Unable to resolve plugin dependencies:");
    for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
      builder.append("\nPlugin: ").append(unresolvedPlugin.getName()).append(" missing dependencies:");
      List<String> missingDependencies = new ArrayList<>();
      for (String dependency : unresolvedPlugin.getClassLoaderModel().getDependencies()) {
        final ArtifactPluginDescriptor dependencyDescriptor = findArtifactPluginDescriptor(dependency, resolvedPlugins);
        if (dependencyDescriptor == null) {
          missingDependencies.add(dependency);
        }
      }

      builder.append(missingDependencies);
    }

    return builder.toString();
  }

  private boolean isResolvedPlugin(ArtifactPluginDescriptor descriptor, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean isResolved = descriptor.getClassLoaderModel().getDependencies().isEmpty();

    if (!isResolved && hasDependenciesResolved(descriptor.getClassLoaderModel().getDependencies(), resolvedPlugins)) {
      isResolved = true;
    }

    return isResolved;
  }

  private static ArtifactPluginDescriptor findArtifactPluginDescriptor(String name,
                                                                       List<ArtifactPluginDescriptor> resolvedPlugins) {
    ArtifactPluginDescriptor result = null;

    for (ArtifactPluginDescriptor resolvedPlugin : resolvedPlugins) {
      if (resolvedPlugin.getName().equals(name)) {
        result = resolvedPlugin;
        break;
      }
    }

    return result;
  }

  private boolean hasDependenciesResolved(Set<String> pluginDependencies, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean resolvedDependency = true;

    for (String dependency : pluginDependencies) {
      if (findArtifactPluginDescriptor(dependency, resolvedPlugins) == null) {
        resolvedDependency = false;
        break;
      }
    }

    return resolvedDependency;
  }
}
