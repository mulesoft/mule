/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.plugin;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates {@link ArtifactClassLoader} for application or domain plugin descriptors.
 */
public class ArtifactPluginClassLoaderFactory implements ArtifactClassLoaderFactory<ArtifactPluginDescriptor> {


  private final ModuleRepository moduleRepository;

  /**
   * Creates a new factory
   *
   * @param moduleRepository provides access to the modules available on the container. Non null.
   */
  public ArtifactPluginClassLoaderFactory(ModuleRepository moduleRepository) {
    checkArgument(moduleRepository != null, "moduleRepository cannot be null");

    this.moduleRepository = moduleRepository;
  }

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactPluginDescriptor descriptor,
                                    ClassLoader parent,
                                    ClassLoaderLookupPolicy baseLookupPolicy) {
    Map<String, LookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (ArtifactPluginDescriptor dependencyPluginDescriptor : descriptor.getArtifactPluginDescriptors()) {
      if (dependencyPluginDescriptor.getName().equals(descriptor.getName())) {
        continue;
      }

      final LookupStrategy parentFirst = getClassLoaderLookupStrategy(descriptor, dependencyPluginDescriptor);

      for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, parentFirst);
      }
    }

    ContainerOnlyLookupStrategy containerOnlyLookupStrategy = new ContainerOnlyLookupStrategy(this.getClass().getClassLoader());

    for (MuleModule module : moduleRepository.getModules()) {
      if (module.getPrivilegedArtifacts().contains(descriptor.getBundleDescriptor().getArtifactId())) {
        for (String packageName : module.getPrivilegedExportedPackages()) {
          pluginsLookupPolicies.put(packageName, containerOnlyLookupStrategy);
        }
      }
    }

    final ClassLoaderLookupPolicy lookupPolicy = baseLookupPolicy.extend(pluginsLookupPolicies);

    return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderModel().getUrls(), parent, lookupPolicy);
  }

  private LookupStrategy getClassLoaderLookupStrategy(ArtifactPluginDescriptor descriptor,
                                                      ArtifactPluginDescriptor dependencyPluginDescriptor) {
    final LookupStrategy parentFirst;
    if (isDependencyPlugin(descriptor.getClassLoaderModel().getDependencies(), dependencyPluginDescriptor)) {
      parentFirst = PARENT_FIRST;
    } else {
      parentFirst = CHILD_ONLY;
    }
    return parentFirst;
  }

  private boolean isDependencyPlugin(Set<BundleDependency> pluginDependencies,
                                     ArtifactPluginDescriptor dependencyPluginDescriptor) {
    for (BundleDependency pluginDependency : pluginDependencies) {
      if (pluginDependency.getDescriptor().getArtifactId()
          .equals(dependencyPluginDescriptor.getBundleDescriptor().getArtifactId())
          && MULE_PLUGIN_CLASSIFIER.equals(pluginDependency.getDescriptor().getClassifier().get())) {
        return true;
      }
    }

    return false;
  }
}
