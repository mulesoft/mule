/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.VersionUtils;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The goal of this class is to check for if a list of Dependencies are compatible amongst them self Compatibility is defined by
 * semantic versioning
 */
public class MulePluginsCompatibilityValidator {

  /**
   * Validates a list of dependencies to check for incompatibilities
   *
   * @param mulePlugins List of mule plugins dependencies
   * @return
   * @throws org.mule.tools.api.exception.ValidationException if the list of mule plugins contains incompatibilities
   */
  public Map<String, List<BundleDescriptor>> validate(List<BundleDescriptor> mulePlugins) {
    return buildDependencyMap(mulePlugins).entrySet()
        .stream()
        .filter(entry -> entry.getValue().size() > 1 && !areMulePluginVersionCompatible(entry.getValue()))
        .collect(toMap(e -> e.getKey(), e -> e.getValue()));
  }

  private boolean areMulePluginVersionCompatible(List<BundleDescriptor> dependencies) {
    Set<String> majors = dependencies.stream()
        .map(BundleDescriptor::getVersion)
        .map(VersionUtils::getMajor)
        .collect(toSet());
    return majors.size() <= 1;
  }

  private Map<String, List<BundleDescriptor>> buildDependencyMap(List<BundleDescriptor> dependencyList) {
    Map<String, List<BundleDescriptor>> dependencyMap = new HashMap<>();

    for (BundleDescriptor plugin : dependencyList) {
      String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
      dependencyMap.computeIfAbsent(pluginKey, k -> new ArrayList<>());
      dependencyMap.get(pluginKey).add(plugin);
    }

    return dependencyMap;
  }

}
