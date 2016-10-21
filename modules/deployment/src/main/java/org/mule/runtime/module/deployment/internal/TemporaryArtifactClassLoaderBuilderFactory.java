/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;

/**
 * Factory for {@code ArtifactClassLoaderBuilder} instances.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilderFactory {

  private ArtifactPluginRepository applicationPluginRepository;
  private final ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory;

  /**
   * Creates an {@code ArtifactClassLoaderBuilderFactory} to create instances of {@code ArtifactClassLoaderBuilder}.
   * 
   * @param applicationPluginRepository repository for artifacts plugins that are provided by default by the runtime
   * @param artifactPluginClassLoaderFactory creates artifact class loaders from descriptors
   */
  public TemporaryArtifactClassLoaderBuilderFactory(ArtifactPluginRepository applicationPluginRepository,
                                                    ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory) {
    this.applicationPluginRepository = applicationPluginRepository;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
  }

  /**
   * Create a new instance of a builder to create an artifact class loader.
   *
   * @return a new instance of {@code ArtifactClassLoaderBuilder}
   */
  public TemporaryArtifactClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new TemporaryArtifactClassLoaderBuilder(applicationPluginRepository, artifactPluginClassLoaderFactory);
  }

}
