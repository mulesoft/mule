/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.domain;

import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Domain} artifacts
 *
 * @since 4.0
 */
public class DefaultDomainClassLoaderBuilder implements DomainClassLoaderBuilder {

  private ArtifactDescriptor artifactDescriptor;
  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;

  /**
   * Creates a new builder for creating {@link Domain} artifacts.
   *
   * @param artifactClassLoaderResolver resolver that will be used to create the class loader. Non-null
   */
  public DefaultDomainClassLoaderBuilder(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = artifactClassLoaderResolver;
  }

  @Override
  public DomainClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor) {
    this.artifactDescriptor = artifactDescriptor;
    return this;
  }

  /**
   * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create the proper class loader hierarchy
   * and filters so domain classes, resources and plugins are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   */
  @Override
  public MuleDeployableArtifactClassLoader build() {
    return artifactClassLoaderResolver
        .createDomainClassLoader((DomainDescriptor) artifactDescriptor);
  }
}
