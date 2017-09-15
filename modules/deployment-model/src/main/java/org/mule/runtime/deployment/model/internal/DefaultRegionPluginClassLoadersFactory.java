/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the class loaders for plugins that are contained in a given region
 *
 * @since 4.0
 */
public class DefaultRegionPluginClassLoadersFactory implements RegionPluginClassLoadersFactory {

  public static final String PLUGIN_CLASSLOADER_IDENTIFIER = "/plugin/";

  private final ArtifactClassLoaderFactory artifactPluginClassLoaderFactory;
  private final ModuleRepository moduleRepository;

  /**
   * Creates a new factory
   *
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   * @param moduleRepository provides access to the modules available on the container. Non null.
   */
  public DefaultRegionPluginClassLoadersFactory(ArtifactClassLoaderFactory artifactPluginClassLoaderFactory,
                                                ModuleRepository moduleRepository) {
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    checkArgument(moduleRepository != null, "moduleRepository cannot be null");

    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.moduleRepository = moduleRepository;
  }

  @Override
  public List<ArtifactClassLoader> createPluginClassLoaders(ArtifactClassLoader regionClassLoader,
                                                            List<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                                            ClassLoaderLookupPolicy regionOwnerLookupPolicy) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      final String pluginArtifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), artifactPluginDescriptor.getName());

      ClassLoaderLookupPolicy pluginLookupPolicy = createPluginLookupPolicy(classLoaders, artifactPluginDescriptor,
                                                                            regionOwnerLookupPolicy, artifactPluginDescriptors);
      final ArtifactClassLoader artifactClassLoader =
          artifactPluginClassLoaderFactory.create(pluginArtifactId, artifactPluginDescriptor, regionClassLoader.getClassLoader(),
                                                  pluginLookupPolicy);

      classLoaders.add(artifactClassLoader);
    }
    return classLoaders;
  }

  /**
   * @param parentArtifactId identifier of the artifact that owns the plugin. Non empty.
   * @param pluginName name of the plugin. Non empty.
   * @return the unique identifier for the plugin inside the parent artifact.
   */
  public static String getArtifactPluginId(String parentArtifactId, String pluginName) {
    checkArgument(!isEmpty(parentArtifactId), "parentArtifactId cannot be empty");
    checkArgument(!isEmpty(pluginName), "pluginName cannot be empty");

    return parentArtifactId + PLUGIN_CLASSLOADER_IDENTIFIER + pluginName;
  }

  private ClassLoaderLookupPolicy createPluginLookupPolicy(List<ArtifactClassLoader> classLoaders,
                                                           ArtifactPluginDescriptor descriptor,
                                                           ClassLoaderLookupPolicy baseLookupPolicy,
                                                           List<ArtifactPluginDescriptor> artifactPluginDescriptors) {
    Map<String, LookupStrategy> pluginsLookupPolicies = new HashMap<>();
    List<ArtifactPluginDescriptor> pluginDescriptors = getPluginDescriptors(descriptor, artifactPluginDescriptors);
    for (ArtifactPluginDescriptor dependencyPluginDescriptor : pluginDescriptors) {
      if (dependencyPluginDescriptor.getName().equals(descriptor.getName())) {
        continue;
      }

      LookupStrategy lookupStrategy = getClassLoaderLookupStrategy(descriptor, dependencyPluginDescriptor);

      for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, lookupStrategy);
      }


      if (isPrivilegedPluginDependency(descriptor, dependencyPluginDescriptor)) {
        Optional<ArtifactClassLoader> pluginClassLoader = classLoaders.stream().filter(
                                                                                       c -> c.getArtifactDescriptor()
                                                                                           .getBundleDescriptor().getArtifactId()
                                                                                           .equals(dependencyPluginDescriptor
                                                                                               .getBundleDescriptor()
                                                                                               .getArtifactId()))
            .findFirst();
        if (!pluginClassLoader.isPresent()) {
          throw new IllegalStateException("Cannot find classloader for plugin: "
              + dependencyPluginDescriptor.getBundleDescriptor().getArtifactId());
        }
        lookupStrategy = new DelegateOnlyLookupStrategy(pluginClassLoader.get().getClassLoader());

        for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedExportedPackages()) {
          pluginsLookupPolicies.put(exportedPackage, lookupStrategy);
        }
      }

    }

    ContainerOnlyLookupStrategy containerOnlyLookupStrategy = new ContainerOnlyLookupStrategy(this.getClass().getClassLoader());

    for (MuleModule module : moduleRepository.getModules()) {
      if (module.getPrivilegedArtifacts()
          .contains(descriptor.getBundleDescriptor().getGroupId() + ":" + descriptor.getBundleDescriptor().getArtifactId())) {
        for (String packageName : module.getPrivilegedExportedPackages()) {
          pluginsLookupPolicies.put(packageName, containerOnlyLookupStrategy);
        }
      }
    }

    return baseLookupPolicy.extend(pluginsLookupPolicies);
  }

  private List<ArtifactPluginDescriptor> getPluginDescriptors(ArtifactPluginDescriptor descriptor,
                                                              List<ArtifactPluginDescriptor> artifactPluginDescriptors) {
    return artifactPluginDescriptors.stream()
        .filter(d -> isDependencyPlugin(descriptor.getClassLoaderModel().getDependencies(), d)).collect(toList());
  }

  private boolean isPrivilegedPluginDependency(ArtifactPluginDescriptor descriptor,
                                               ArtifactPluginDescriptor dependencyPluginDescriptor) {
    if (dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedExportedPackages().isEmpty()) {
      return false;
    }

    return dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedArtifacts().stream().filter(
                                                                                                     a -> a.startsWith(descriptor
                                                                                                         .getBundleDescriptor()
                                                                                                         .getGroupId()
                                                                                                         + ":"
                                                                                                         + descriptor
                                                                                                             .getBundleDescriptor()
                                                                                                             .getArtifactId()))
        .findFirst().isPresent();
  }

  private LookupStrategy getClassLoaderLookupStrategy(ArtifactPluginDescriptor descriptor,
                                                      ArtifactPluginDescriptor dependencyPluginDescriptor) {
    final LookupStrategy lookupStrategy;
    if (isDependencyPlugin(descriptor.getClassLoaderModel().getDependencies(), dependencyPluginDescriptor)) {
      lookupStrategy = PARENT_FIRST;
    } else {
      lookupStrategy = CHILD_ONLY;
    }
    return lookupStrategy;
  }

  private boolean isDependencyPlugin(Set<BundleDependency> dependencies,
                                     ArtifactPluginDescriptor dependencyPluginDescriptor) {
    for (BundleDependency pluginDependency : dependencies) {
      if (pluginDependency.getDescriptor().getArtifactId()
          .equals(dependencyPluginDescriptor.getBundleDescriptor().getArtifactId())
          && pluginDependency.getDescriptor().getGroupId()
              .equals(dependencyPluginDescriptor.getBundleDescriptor().getGroupId())
          && MULE_PLUGIN_CLASSIFIER.equals(pluginDependency.getDescriptor().getClassifier().orElse(null))) {
        return true;
      }
    }

    return false;
  }

}
