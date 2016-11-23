/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.test.runner.api.PluginUrlClassification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for plugins {@link ClassLoaderLookupPolicy}.
 *
 * @since 4.0
 */
public class PluginLookPolicyFactory {

  /**
   * Creates a {@link ClassLoaderLookupPolicy} for plugins considering their dependencies.
   *
   * @param pluginClassification the {@link PluginUrlClassification} to creates its {@link ClassLoaderLookupPolicy}
   * @param pluginClassifications whole list of {@link PluginUrlClassification} for the current context
   * @param parentLookupPolicies the {@link ClassLoaderLookupPolicy} for the parent {@link ClassLoader}
   * @return {@link ClassLoaderLookupPolicy} for the plugin
   */
  public ClassLoaderLookupPolicy createLookupPolicy(PluginUrlClassification pluginClassification,
                                                    List<PluginUrlClassification> pluginClassifications,
                                                    ClassLoaderLookupPolicy parentLookupPolicies) {
    Map<String, ClassLoaderLookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (PluginUrlClassification dependencyPluginClassification : pluginClassifications) {
      if (dependencyPluginClassification.getArtifactId().equals(pluginClassification.getArtifactId())) {
        continue;
      }

      ClassLoaderLookupStrategy lookUpPolicyStrategy =
          getClassLoaderLookupStrategy(pluginClassification, dependencyPluginClassification);

      for (String exportedPackage : dependencyPluginClassification.getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, lookUpPolicyStrategy);
      }

    }
    return parentLookupPolicies.extend(pluginsLookupPolicies);
  }

  /**
   * If the plugin declares the dependency the {@link PluginUrlClassification} would be {@link ClassLoaderLookupStrategy#PARENT_FIRST}
   * otherwise {@link ClassLoaderLookupStrategy#CHILD_FIRST}.
   *
   * @param currentPluginClassification {@link PluginUrlClassification} being classified.
   * @param dependencyPluginClassification {@link PluginUrlClassification} from the region.
   * @return {@link ClassLoaderLookupStrategy} to be used by current plugin for the exported packages defined by the dependencyPluginClassification.
   */
  private ClassLoaderLookupStrategy getClassLoaderLookupStrategy(PluginUrlClassification currentPluginClassification,
                                                                 PluginUrlClassification dependencyPluginClassification) {
    final ClassLoaderLookupStrategy parentFirst;
    if (currentPluginClassification.getPluginDependencies().contains(dependencyPluginClassification.getName())) {
      parentFirst = PARENT_FIRST;
    } else {
      parentFirst = CHILD_ONLY;
    }
    return parentFirst;
  }
}
