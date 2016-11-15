/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.Collections.emptyList;
import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.util.List;

/**
 * Factory for {@code ArtifactClassLoaderBuilder} instances.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilderFactory {

  private ArtifactPluginRepository applicationPluginRepository;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory;
  private final DependenciesProvider dependenciesProvider;

  /**
   * Creates an {@code ArtifactClassLoaderBuilderFactory} to create instances of {@code ArtifactClassLoaderBuilder}.
   * 
   * @param applicationPluginRepository repository for artifacts plugins that are provided by default by the runtime
   * @param artifactPluginClassLoaderFactory creates artifact class loaders from descriptors
   * @param artifactDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency to resolve
   * @param dependenciesProvider resolver for missing dependencies
   */
  public TemporaryArtifactClassLoaderBuilderFactory(ArtifactPluginRepository applicationPluginRepository,
                                                    ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory,
                                                    ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory,
                                                    DependenciesProvider dependenciesProvider) {
    this.applicationPluginRepository = new EmptyArtifactPluginRepository();
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.artifactDescriptorFactory = artifactDescriptorFactory;
    this.dependenciesProvider = dependenciesProvider;
  }

  /**
   * Create a new instance of a builder to create an artifact class loader.
   *
   * @return a new instance of {@code ArtifactClassLoaderBuilder}
   */
  public TemporaryArtifactClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new TemporaryArtifactClassLoaderBuilder(applicationPluginRepository, artifactPluginClassLoaderFactory,
                                                   artifactDescriptorFactory, dependenciesProvider);
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
