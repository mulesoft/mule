/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Domain} artifacts
 *
 * @since 4.0
 */
public class DefaultDomainClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<DefaultDomainClassLoaderBuilder>
    implements DomainClassLoaderBuilder {

  private final ArtifactClassLoader parentClassLoader;
  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;

  /**
   * Creates a new builder for creating {@link Domain} artifacts.
   *
   * @param parentClassLoader          classloader that will be the parent of the created classloaders. Non null
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes
   * @param pluginClassLoadersFactory  creates the class loaders for the plugins included in the domain's region. Non null
   */
  public DefaultDomainClassLoaderBuilder(ArtifactClassLoader parentClassLoader,
                                         DeployableArtifactClassLoaderFactory<DomainDescriptor> artifactClassLoaderFactory,
                                         RegionPluginClassLoadersFactory pluginClassLoadersFactory) {
    super(pluginClassLoadersFactory);
    this.parentClassLoader = parentClassLoader;
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }

  /**
   * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create the proper class loader hierarchy
   * and filters so domain classes, resources and plugins are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   */
  @Override
  public MuleDeployableArtifactClassLoader build() {
    return (MuleDeployableArtifactClassLoader) super.build();
  }

  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return getDomainId(artifactDescriptor.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }

  /**
   * @param domainName name of the domain. Non empty.
   * @return the unique identifier for the domain in the container.
   */
  public static String getDomainId(String domainName) {
    checkArgument(!isEmpty(domainName), "domainName cannot be empty");

    return "/domain/" + domainName;
  }
}
