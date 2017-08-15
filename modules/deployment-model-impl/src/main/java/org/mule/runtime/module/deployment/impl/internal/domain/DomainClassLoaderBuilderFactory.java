/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.internal.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;

public class DomainClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final ArtifactClassLoader parentClassLoader;
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;

  /**
   * Creates an {@code DomainClassLoaderBuilderFactory} to create {@code DomainClassLoaderBuilder} instances.
   *
   * @param parentClassLoader classloader that will be the parent of the created classloaders. Non null
   * @param domainClassLoaderFactory factory for the class loader of the artifact resources and classes
   * @param pluginClassLoadersFactory creates the class loaders for the plugins included in the domain's region. Non null
   */
  public DomainClassLoaderBuilderFactory(ArtifactClassLoader parentClassLoader,
                                         DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory,
                                         RegionPluginClassLoadersFactory pluginClassLoadersFactory) {
    checkArgument(pluginClassLoadersFactory != null, "pluginClassLoadersFactory cannot be null");
    this.parentClassLoader = parentClassLoader;
    this.pluginClassLoadersFactory = pluginClassLoadersFactory;
    this.domainClassLoaderFactory = domainClassLoaderFactory;
  }

  /**
   * Creates a new {@code DomainClassLoaderBuilder} instance to create the domain artifact class loader.
   *
   * @return a {@code DomainClassLoaderBuilder} instance.
   */
  public DomainClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new DomainClassLoaderBuilder(parentClassLoader, domainClassLoaderFactory, pluginClassLoadersFactory);
  }

}
