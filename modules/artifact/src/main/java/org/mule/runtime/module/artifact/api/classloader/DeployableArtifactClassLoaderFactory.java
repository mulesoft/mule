/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.emptyList;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.List;

/**
 * Creates {@link ClassLoader} instances for deployable artifacts
 *
 * @since 4.0
 * @deprecated in 4.5, use org.mule.runtime.artifact.activation.api.ArtifactClassLoaderResolver instead.
 */
@NoImplement
@Deprecated
public interface DeployableArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a {@link ClassLoader} from a given descriptor
   *
   * @param artifactId artifact unique ID
   * @param parent     parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @return a new classLoader for described artifact
   */
  default ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, T descriptor) {
    return create(artifactId, parent, descriptor, emptyList());
  }

  /**
   * Creates a {@link ClassLoader} from a given descriptor
   *
   * @param artifactId                 artifact unique ID
   * @param parent                     parent for the new artifact classloader.
   * @param descriptor                 descriptor of the artifact owner of the created classloader
   * @param artifactPluginClassLoaders {@link List} with the artifact plugin class loaders
   * @return a new classLoader for described artifact
   *
   * @deprecated use {@link #create(String, ArtifactClassLoader, ArtifactDescriptor)} instead.
   */
  @Deprecated
  ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, T descriptor,
                             List<ArtifactClassLoader> artifactPluginClassLoaders);

}
