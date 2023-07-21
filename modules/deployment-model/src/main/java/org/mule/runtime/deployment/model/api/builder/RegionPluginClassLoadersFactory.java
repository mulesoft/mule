/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.builder;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.List;

/**
 * Creates the class loaders for the plugins included in a given region.
 *
 * @since 4.0
 */
public interface RegionPluginClassLoadersFactory {

  /**
   * Creates all the region plugin class loaders
   *
   * @param regionClassLoader         region classloader
   * @param artifactPluginDescriptors resolved descriptors of all the plugins to include in the region.
   * @param regionOwnerLookupPolicy   lookup policy used as the base to create plugin's lookup policy
   * @return a non null {@link List} containing a classloader for each plugin descriptor.
   */
  List<ArtifactClassLoader> createPluginClassLoaders(ArtifactClassLoader regionClassLoader,
                                                     List<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                                     ClassLoaderLookupPolicy regionOwnerLookupPolicy);
}
