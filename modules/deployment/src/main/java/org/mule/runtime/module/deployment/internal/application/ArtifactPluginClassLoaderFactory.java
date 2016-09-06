/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.application;

import static java.lang.System.arraycopy;

import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates {@link ArtifactClassLoader} for application or domain plugin descriptors.
 */
public class ArtifactPluginClassLoaderFactory implements ArtifactClassLoaderFactory<ArtifactPluginDescriptor> {

  /**
   * @param parent parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @return an {@link ArtifactClassLoader} for the given {@link ArtifactPluginDescriptor}
   */
  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ArtifactPluginDescriptor descriptor) {
    URL[] urls = new URL[descriptor.getRuntimeLibs().length + 1];
    urls[0] = descriptor.getRuntimeClassesDir();
    arraycopy(descriptor.getRuntimeLibs(), 0, urls, 1, descriptor.getRuntimeLibs().length);

    Map<String, ClassLoaderLookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (ArtifactPluginDescriptor dependencyPluginDescriptor : descriptor.getArtifactPluginDescriptors()) {
      if (dependencyPluginDescriptor.getName().equals(descriptor.getName())) {
        continue;
      }

      final ClassLoaderLookupStrategy parentFirst = getClassLoaderLookupStrategy(descriptor, dependencyPluginDescriptor);

      for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderFilter().getExportedClassPackages()) {
        pluginsLookupPolicies.put(exportedPackage, parentFirst);
      }
    }

    final ClassLoaderLookupPolicy lookupPolicy = parent.getClassLoaderLookupPolicy().extend(pluginsLookupPolicies);

    return new MuleArtifactClassLoader(descriptor.getName(), urls, parent.getClassLoader(), lookupPolicy);
  }

  private ClassLoaderLookupStrategy getClassLoaderLookupStrategy(ArtifactPluginDescriptor descriptor,
                                                                 ArtifactPluginDescriptor dependencyPluginDescriptor) {
    final ClassLoaderLookupStrategy parentFirst;
    if (isDependencyPlugin(descriptor.getPluginDependencies(), dependencyPluginDescriptor)) {
      parentFirst = ClassLoaderLookupStrategy.PARENT_FIRST;
    } else {
      parentFirst = ClassLoaderLookupStrategy.CHILD_ONLY;
    }
    return parentFirst;
  }

  private boolean isDependencyPlugin(Set<String> pluginDependencies, ArtifactPluginDescriptor dependencyPluginDescriptor) {
    return pluginDependencies.contains(dependencyPluginDescriptor.getName());
  }
}
