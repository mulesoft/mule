/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.LookupStrategy;
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

      if (pluginClassification.getPluginDependencies().contains(dependencyPluginClassification.getName())) {
        LookupStrategy lookUpPolicyStrategy = PARENT_FIRST;

        for (String exportedPackage : dependencyPluginClassification.getExportedPackages()) {
          pluginsLookupPolicies.put(exportedPackage, lookUpPolicyStrategy);
        }
      }
    }
    return parentLookupPolicies.extend(pluginsLookupPolicies);
  }

}
