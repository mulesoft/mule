/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.application.ApplicationClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

/**
 * Factory to create instances of {@code ApplicationClassLoaderBuilder}.
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory;
  private final ArtifactPluginRepository artifactPluginRepository;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory;
  private final DependenciesProvider dependenciesProvider;

  /**
   * Creates an {@code ApplicationClassLoaderBuilderFactory} to create {@code ApplicationClassLoaderBuilder} instances.
   * 
   * @param applicationClassLoaderFactory factory for the class loader of the artifact resources and classes
   * @param artifactPluginRepository repository for artifact plugins provided by the runtime
   * @param artifactPluginClassLoaderFactory creates artifact plugin class loaders. Non null.
   * @param artifactDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency to resolve
   * @param dependenciesProvider resolver for missing dependencies
   */
  public ApplicationClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory,
                                              ArtifactPluginRepository artifactPluginRepository,
                                              ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory,
                                              ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactDescriptorFactory,
                                              DependenciesProvider dependenciesProvider) {
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    this.applicationClassLoaderFactory = applicationClassLoaderFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.artifactDescriptorFactory = artifactDescriptorFactory;
    this.dependenciesProvider = dependenciesProvider;
  }

  /**
   * Creates a new {@code ApplicationClassLoaderBuilder} instance to create the application artifact class loader.
   *
   * @return a {@code ApplicationClassLoaderBuilder} instance.
   */
  public ApplicationClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new ApplicationClassLoaderBuilder(applicationClassLoaderFactory, artifactPluginRepository,
                                             artifactPluginClassLoaderFactory, artifactDescriptorFactory, dependenciesProvider);
  }

}
