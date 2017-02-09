/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static org.mule.runtime.module.artifact.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ChildOnlyLookupStrategy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy;
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
    Map<String, LookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (PluginUrlClassification dependencyPluginClassification : pluginClassifications) {
      if (dependencyPluginClassification.getArtifactId().equals(pluginClassification.getArtifactId())) {
        continue;
      }

      LookupStrategy lookUpPolicyStrategy = getClassLoaderLookupStrategy(pluginClassification, dependencyPluginClassification);

      for (String exportedPackage : dependencyPluginClassification.getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, lookUpPolicyStrategy);
      }

    }
    return parentLookupPolicies.extend(pluginsLookupPolicies);
  }

  /**
   * If the plugin declares the dependency the {@link PluginUrlClassification} would be {@link ParentFirstLookupStrategy}
   * otherwise {@link ChildOnlyLookupStrategy}.
   *
   * @param currentPluginClassification {@link PluginUrlClassification} being classified.
   * @param dependencyPluginClassification {@link PluginUrlClassification} from the region.
   * @return {@link LookupStrategy} to be used by current plugin for the exported packages defined by the dependencyPluginClassification.
   */
  private LookupStrategy getClassLoaderLookupStrategy(PluginUrlClassification currentPluginClassification,
                                                      PluginUrlClassification dependencyPluginClassification) {
    final LookupStrategy parentFirst;
    if (currentPluginClassification.getPluginDependencies().contains(dependencyPluginClassification.getName())) {
      parentFirst = PARENT_FIRST;
    } else {
      parentFirst = CHILD_ONLY;
    }
    return parentFirst;
  }
}
