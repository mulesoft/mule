/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Application} artifacts
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<ApplicationClassLoaderBuilder> {

  private Domain domain;

  /**
   * Creates a new builder for creating {@link Application} artifacts.
   * <p>
   * The {@code domainRepository} is used to locate the domain that this application belongs to and the
   * {@code artifactClassLoaderBuilder} is used for building the common parts of artifacts.
   *  @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes
   * @param artifactPluginRepository repository of plugins contained by the runtime
   * @param artifactPluginClassLoaderFactory creates artifact plugin class loaders.
   */
  public ApplicationClassLoaderBuilder(MuleApplicationClassLoaderFactory artifactClassLoaderFactory,
                                       ArtifactPluginRepository artifactPluginRepository,
                                       ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory) {
    super(artifactClassLoaderFactory, artifactPluginRepository, artifactPluginClassLoaderFactory);
  }

  /**
   * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create the proper class loader hierarchy
   * and filters so application classes, resources, plugins and it's domain resources are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   * @throws IOException exception cause when it was not possible to access the file provided as dependencies
   */
  public MuleDeployableArtifactClassLoader build() throws IOException {
    checkState(domain != null, "Domain cannot be null");

    return (MuleDeployableArtifactClassLoader) super.build();
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return getApplicationId(domain.getArtifactId(), artifactDescriptor.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return this.domain.getArtifactClassLoader();
  }

  /**
   * @param domain the domain artifact to which the application that is going to use this classloader belongs.
   * @return the builder
   */
  public ApplicationClassLoaderBuilder setDomain(Domain domain) {
    this.domain = domain;
    return this;
  }

  public static String getApplicationId(String domainId, String applicationName) {
    return domainId + "/app/" + applicationName;
  }

}
