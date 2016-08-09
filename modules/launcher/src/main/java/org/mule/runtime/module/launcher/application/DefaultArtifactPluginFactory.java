/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

/**
 * Default implementation for creating an {@link ArtifactPlugin} with the corresponding classloader.
 *
 * @since 4.0
 */
public class DefaultArtifactPluginFactory implements ArtifactPluginFactory {

  private ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory;

  /**
   * Creates an instance
   *
   * @param artifactPluginClassLoaderFactory used to create the {@link ArtifactClassLoader}, cannot be null.
   */
  public DefaultArtifactPluginFactory(ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory) {
    checkArgument(artifactPluginClassLoaderFactory != null, "Application plugin classloader factory cannot be null");
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
  }

  @Override
  public ArtifactPlugin create(ArtifactPluginDescriptor descriptor, ArtifactClassLoader parent) {
    final ArtifactClassLoader pluginClassLoader = artifactPluginClassLoaderFactory.create(parent, descriptor);

    return new DefaultArtifactPlugin(descriptor, pluginClassLoader);
  }
}
