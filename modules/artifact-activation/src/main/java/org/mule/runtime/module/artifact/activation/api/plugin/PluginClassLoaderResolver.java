/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves the plugin class loader corresponding to the given descriptor and within the given application or domain.
 *
 * @since 4.5
 */
public interface PluginClassLoaderResolver {

  /**
   * Optionally generates a {@link Supplier} for a plugin class loader given the plugin's {@link ArtifactPluginDescriptor
   * descriptor} and owner artifact's class loader.
   *
   * @param ownerArtifactClassLoader the classLoader for the artifact that has the plugin dependency for the target classLoader.
   * @param artifactPluginDescriptor the descriptor of the plugin to generate a classLoader for.
   * @return optionally returns a {@link Supplier} for a plugin class loader within the given application or domain.
   */
  Optional<Supplier<ArtifactClassLoader>> resolve(ArtifactClassLoader ownerArtifactClassLoader,
                                                  ArtifactPluginDescriptor artifactPluginDescriptor);

}
