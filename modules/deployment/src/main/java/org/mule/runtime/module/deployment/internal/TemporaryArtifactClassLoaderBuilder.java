/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders used by mule artifacts such as domains or applications.
 *
 * Allows to construct a classloader when using a set of artifact plugins and takes into account default plugins provided by the
 * runtime and the shared libraries configured for the plugins.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<TemporaryArtifactClassLoaderBuilder> {

  private ArtifactClassLoader parentClassLoader;

  /**
   * Creates an {@link TemporaryArtifactClassLoaderBuilder}.
   * 
   * @param artifactPluginRepository repository of plugins contained by the runtime. Must be not null.
   * @param artifactPluginClassLoaderFactory factory for creating class loaders for artifact plugins. Must be not null.
   */
  public TemporaryArtifactClassLoaderBuilder(ArtifactPluginRepository artifactPluginRepository,
                                             ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory) {
    super(new TemporaryArtifactClassLoaderFactory(), artifactPluginRepository,
          artifactPluginClassLoaderFactory);
  }

  /**
   * @param parentClassLoader parent class loader for the artifact class loader.
   * @return the builder
   */
  public TemporaryArtifactClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleDeployableArtifactClassLoader build() throws IOException {
    setDeployableArtifactDescriptor(new DeployableArtifactDescriptor("temp"));
    return (MuleDeployableArtifactClassLoader) super.build();
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return "temp/" + artifactDescriptor.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }
}
