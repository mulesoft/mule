/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;

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
   * @param regionClassLoader region classloader
   * @param artifactPluginDescriptors resolved descriptors of all the plugins to include in the region.
   * @param regionOwnerLookupPolicy lookup policy used as the base to create plugin's lookup policy
   * @return a non null {@link List} containing a classloader for each plugin descriptor.
   */
  List<ArtifactClassLoader> createPluginClassLoaders(ArtifactClassLoader regionClassLoader,
                                                     List<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                                     ClassLoaderLookupPolicy regionOwnerLookupPolicy);
}
