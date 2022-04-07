/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Application} artifacts
 *
 * @since 4.0
 */
public class DefaultApplicationClassLoaderBuilder implements ApplicationClassLoaderBuilder {

  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;
  private ArtifactClassLoader domainArtifactClassLoader;
  private ArtifactDescriptor artifactDescriptor;

  /**
   * Creates a new builder for creating {@link Application} artifacts.
   * <p>
   * The {@code domainRepository} is used to locate the domain that this application belongs to and the
   * {@code artifactClassLoaderBuilder} is used for building the common parts of artifacts.
   *
   * @param artifactClassLoaderResolver resolver that will be used to create the class loader. Non-null
   */
  public DefaultApplicationClassLoaderBuilder(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = artifactClassLoaderResolver;
  }

  @Override
  public ApplicationClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor) {
    this.artifactDescriptor = artifactDescriptor;
    return this;
  }

  /**
   * Creates a new {@code MuleDeployableArtifactClassLoader} using the provided configuration. It will create the proper class
   * loader hierarchy and filters so application classes, resources, plugins and it's domain resources are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   * @throws IOException exception cause when it was not possible to access the file provided as dependencies
   */
  @Override
  public MuleDeployableArtifactClassLoader build() {
    checkState(domainArtifactClassLoader != null, "Domain cannot be null");

    return artifactClassLoaderResolver
        .createApplicationClassLoader((ApplicationDescriptor) artifactDescriptor, () -> domainArtifactClassLoader);
  }

  /**
   * @param domainArtifactClassLoader the domain artifact to which the application that is going to use this classloader belongs.
   * @return the builder
   */
  @Override
  public DefaultApplicationClassLoaderBuilder setDomainParentClassLoader(ArtifactClassLoader domainArtifactClassLoader) {
    this.domainArtifactClassLoader = domainArtifactClassLoader;
    return this;
  }
}
