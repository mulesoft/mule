/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
