/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;

/**
 * Base {@link ArtifactClassLoader} implementation of deployable artifacts.
 *
 * @since 4.0
 */
public class MuleDeployableArtifactClassLoader extends MuleArtifactClassLoader {

  private final List<ArtifactClassLoader> artifactPluginClassLoaders;

  static {
    registerAsParallelCapable();
  }

  /**
   * Creates a {@link MuleDeployableArtifactClassLoader} with the provided configuration.
   *
   * @param artifactId artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance.
   * @param urls the URLs from which to load classes and resources
   * @param parent parent class loader in the hierarchy
   * @param lookupPolicy policy for resolving classes and resources
   * @param artifactPluginClassLoaders class loaders for the plugin artifacts contained by this artifact. Must be not null.
   */
  public MuleDeployableArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls,
                                           ClassLoader parent,
                                           ClassLoaderLookupPolicy lookupPolicy,
                                           List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
    checkArgument(artifactPluginClassLoaders != null, "artifact plugin class loaders cannot be null");
    this.artifactPluginClassLoaders = artifactPluginClassLoaders;
  }

  /**
   * Provides a {@link List} with the plugin name as key and its classloader as value.
   *
   * @return {@link List} of plugin class loaders
   */
  public List<ArtifactClassLoader> getArtifactPluginClassLoaders() {
    return artifactPluginClassLoaders;
  }
}
