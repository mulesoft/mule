/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Resolves plugin dependencies considering the plugin name only.
 */
public class NamePluginDependenciesResolver implements PluginDependenciesResolver {

  @Override
  public List<ArtifactPluginDescriptor> resolve(List<ArtifactPluginDescriptor> descriptors) {
    descriptors.sort((d1, d2) -> (d1.getName().compareTo(d2.getName())));

    List<ArtifactPluginDescriptor> resolvedPlugins = new LinkedList<>();
    List<ArtifactPluginDescriptor> unresolvedPlugins = new LinkedList<>(descriptors);

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedPlugins.size();

      List<ArtifactPluginDescriptor> pendingUnresolvedPlugins = new LinkedList<>();

      for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
        if (isResolvedPlugin(unresolvedPlugin, resolvedPlugins)) {
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
      throw new PluginResolutionError(createResolutionErrorMessage(unresolvedPlugins));
    }

    return resolvedPlugins;
  }

  protected static String createResolutionErrorMessage(List<ArtifactPluginDescriptor> unresolvedPlugins) {
    return format("Unable to resolve plugin dependencies: %s", unresolvedPlugins);
  }

  private boolean isResolvedPlugin(ArtifactPluginDescriptor descriptor, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean isResolved = descriptor.getPluginDependencies().isEmpty();

    if (!isResolved && hasDependenciesResolved(descriptor.getPluginDependencies(), resolvedPlugins)) {
      isResolved = true;
    }

    return isResolved;
  }

  private boolean hasDependenciesResolved(Set<String> pluginDependencies, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean resolvedDependency = true;

    for (String dependency : pluginDependencies) {
      resolvedDependency = false;
      for (ArtifactPluginDescriptor resolvedPlugin : resolvedPlugins) {

        if (resolvedPlugin.getName().equals(dependency)) {
          resolvedDependency = true;
        }
      }

      if (!resolvedDependency) {
        break;
      }
    }

    return resolvedDependency;
  }
}
