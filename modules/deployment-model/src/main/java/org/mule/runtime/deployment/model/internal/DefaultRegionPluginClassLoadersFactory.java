/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.deployment.model.api.builder.RegionPluginClassLoadersFactory;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.LinkedList;
import java.util.List;

/**
 * Creates the class loaders for plugins that are contained in a given region
 *
 * @since 4.0
 */
public class DefaultRegionPluginClassLoadersFactory implements RegionPluginClassLoadersFactory {

  public static final String PLUGIN_CLASSLOADER_IDENTIFIER = "/plugin/";

  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;

  /**
   * Creates a new factory
   *
   * @param artifactClassLoaderResolver resolver that will be used to create the class loader. Non-null
   */
  public DefaultRegionPluginClassLoadersFactory(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = requireNonNull(artifactClassLoaderResolver);
  }

  @Override
  public List<ArtifactClassLoader> createPluginClassLoaders(ArtifactClassLoader regionClassLoader,
                                                            List<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                                            ClassLoaderLookupPolicy regionOwnerLookupPolicy) {
    List<ArtifactClassLoader> classLoaders = new LinkedList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors) {
      final ArtifactClassLoader artifactClassLoader =
          artifactClassLoaderResolver
                .createMulePluginClassLoader((MuleDeployableArtifactClassLoader) ((RegionClassLoader) regionClassLoader)
                  .getOwnerClassLoader(),
                                           artifactPluginDescriptor,
                                           (pluginDescriptors, bundleDescriptor) -> artifactPluginDescriptors
                                               .stream()
                                               .filter(apd -> apd.getBundleDescriptor()
                                                   .getArtifactId()
                                                   .equals(bundleDescriptor.getArtifactId())
                                                   && apd.getBundleDescriptor().getGroupId()
                                                       .equals(bundleDescriptor.getGroupId()))
                                               .findAny(),
                                           (ownerArtifactClassLoader, dependencyPluginDescriptor) -> of(() -> classLoaders
                                               .stream().filter(
                                                                c -> c
                                                                    .getArtifactDescriptor()
                                                                    .getBundleDescriptor()
                                                                    .getArtifactId()
                                                                    .equals(dependencyPluginDescriptor
                                                                        .getBundleDescriptor()
                                                                        .getArtifactId()))
                                               .findAny().get()));

      classLoaders.add(artifactClassLoader);
    }
    return classLoaders;
  }

  /**
   * @param parentArtifactId identifier of the artifact that owns the plugin. Non empty.
   * @param pluginName       name of the plugin. Non empty.
   * @return the unique identifier for the plugin inside the parent artifact.
   */
  public static String getArtifactPluginId(String parentArtifactId, String pluginName) {
    checkArgument(!isEmpty(parentArtifactId), "parentArtifactId cannot be empty");
    checkArgument(!isEmpty(pluginName), "pluginName cannot be empty");

    return parentArtifactId + PLUGIN_CLASSLOADER_IDENTIFIER + pluginName;
  }
}
