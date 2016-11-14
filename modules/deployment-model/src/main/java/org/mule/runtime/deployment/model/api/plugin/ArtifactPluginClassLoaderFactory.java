/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.plugin;

import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates {@link ArtifactClassLoader} for application or domain plugin descriptors.
 */
public class ArtifactPluginClassLoaderFactory implements ArtifactClassLoaderFactory<ArtifactPluginDescriptor> {

  /**
   * @param artifactId artifact unique ID. Non empty.
   * @param parent parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @return an {@link ArtifactClassLoader} for the given {@link ArtifactPluginDescriptor}
   */
  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, ArtifactPluginDescriptor descriptor) {
    Map<String, ClassLoaderLookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (ArtifactPluginDescriptor dependencyPluginDescriptor : descriptor.getArtifactPluginDescriptors()) {
      if (dependencyPluginDescriptor.getName().equals(descriptor.getName())) {
        continue;
      }

      final ClassLoaderLookupStrategy parentFirst = getClassLoaderLookupStrategy(descriptor, dependencyPluginDescriptor);

      for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, parentFirst);
      }
    }

    final ClassLoaderLookupPolicy lookupPolicy = parent.getClassLoaderLookupPolicy().extend(pluginsLookupPolicies);

    return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderModel().getUrls(),
                                       parent.getClassLoader(), lookupPolicy);
  }

  private ClassLoaderLookupStrategy getClassLoaderLookupStrategy(ArtifactPluginDescriptor descriptor,
                                                                 ArtifactPluginDescriptor dependencyPluginDescriptor) {
    final ClassLoaderLookupStrategy parentFirst;
    if (isDependencyPlugin(descriptor.getClassLoaderModel().getDependencies(), dependencyPluginDescriptor)) {
      parentFirst = ClassLoaderLookupStrategy.PARENT_FIRST;
    } else {
      parentFirst = ClassLoaderLookupStrategy.CHILD_ONLY;
    }
    return parentFirst;
  }

  private boolean isDependencyPlugin(Set<BundleDependency> pluginDependencies,
                                     ArtifactPluginDescriptor dependencyPluginDescriptor) {
    for (BundleDependency pluginDependency : pluginDependencies) {
      if (pluginDependency.getDescriptor().getArtifactId().equals(dependencyPluginDescriptor.getName())
          && MULE_PLUGIN_CLASSIFIER.equals(pluginDependency.getClassifier().get())) {
        return true;
      }
    }

    return false;
  }
}
