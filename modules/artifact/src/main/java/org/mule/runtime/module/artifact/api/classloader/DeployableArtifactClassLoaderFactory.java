/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.List;

/**
 * Creates {@link ClassLoader} instances for deployable artifacts
 *
 * @since 4.0
 */
public interface DeployableArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a {@link ClassLoader} from a given descriptor
   *
   * @param artifactId artifact unique ID
   * @param parent parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @param artifactPluginClassLoaders {@link List} with the artifact plugin class loaders
   * @return a new classLoader for described artifact
   */
  ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, T descriptor,
                             List<ArtifactClassLoader> artifactPluginClassLoaders);

}
