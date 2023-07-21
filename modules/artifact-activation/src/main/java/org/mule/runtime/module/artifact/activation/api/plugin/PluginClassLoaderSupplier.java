/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

/**
 * Supplies a plugin class loader based on its descriptor.
 *
 * @since 4.5
 */
public interface PluginClassLoaderSupplier {

  /**
   * Supplies the class loader corresponding to the given plugin descriptor.
   *
   * @param artifactPluginDescriptor the descriptor of the plugin to supply a classLoader for.
   * @return the class loader corresponding to the given plugin descriptor.
   */
  ArtifactClassLoader get(ArtifactPluginDescriptor artifactPluginDescriptor);

}
