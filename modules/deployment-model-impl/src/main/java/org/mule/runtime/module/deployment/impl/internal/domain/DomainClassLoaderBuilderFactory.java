/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

public class DomainClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final ArtifactClassLoader parentClassLoader;

  /**
   * Creates an {@code DomainClassLoaderBuilderFactory} to create {@code DomainClassLoaderBuilder} instances.
   *
   * @param parentClassLoader classloader that will be the parent of the created classloaders. Non null
   * @param domainClassLoaderFactory factory for the class loader of the artifact resources and classes
   * @param artifactPluginClassLoaderFactory creates artifact plugin class loaders. Non null.
   */
  public DomainClassLoaderBuilderFactory(ArtifactClassLoader parentClassLoader,
                                         DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory,
                                         ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory) {
    this.parentClassLoader = parentClassLoader;
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    this.domainClassLoaderFactory = domainClassLoaderFactory;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
  }

  /**
   * Creates a new {@code DomainClassLoaderBuilder} instance to create the domain artifact class loader.
   *
   * @return a {@code DomainClassLoaderBuilder} instance.
   */
  public DomainClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new DomainClassLoaderBuilder(parentClassLoader, domainClassLoaderFactory, artifactPluginClassLoaderFactory);
  }

}
