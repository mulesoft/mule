/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.temporary;

import static java.util.Collections.emptyList;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

import java.util.List;

/**
 * Factory for {@code ArtifactClassLoaderBuilder} instances.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilderFactory {

  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final DeployableArtifactClassLoaderFactory<ApplicationDescriptor> artifactClassLoaderFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;

  /**
   * Creates an {@code ArtifactClassLoaderBuilderFactory} to create instances of {@code ArtifactClassLoaderBuilder}.
   * 
   * @param artifactPluginClassLoaderFactory creates artifact plugin class loaders from descriptors
   * @param artifactClassLoaderFactory creates artifact class loaders from descriptors
   * @param pluginDependenciesResolver resolves artifact plugin dependencies. Non null
   */
  public TemporaryArtifactClassLoaderBuilderFactory(ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory,
                                                    DeployableArtifactClassLoaderFactory<ApplicationDescriptor> artifactClassLoaderFactory,
                                                    PluginDependenciesResolver pluginDependenciesResolver) {
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
  }

  /**
   * Create a new instance of a builder to create an artifact class loader.
   *
   * @return a new instance of {@code ArtifactClassLoaderBuilder}
   */
  public TemporaryArtifactClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new TemporaryArtifactClassLoaderBuilder(artifactPluginClassLoaderFactory,
                                                   artifactClassLoaderFactory);
  }

  /**
   * For temporary artifacts the runtime shouldn't have any plugin already registered.
   */
  private class EmptyArtifactPluginRepository implements ArtifactPluginRepository {

    @Override
    public List<ArtifactPluginDescriptor> getContainerArtifactPluginDescriptors() {
      return emptyList();
    }

  }

}
