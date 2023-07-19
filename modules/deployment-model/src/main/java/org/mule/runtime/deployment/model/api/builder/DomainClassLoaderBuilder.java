/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.builder;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Domain} artifacts
 *
 * @since 4.5
 */
public interface DomainClassLoaderBuilder {

  /**
   * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
   * @return the builder
   */
  DomainClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor);

  /**
   * Creates a new {@code MuleDeployableArtifactClassLoader} using the provided configuration. It will create the proper class
   * loader hierarchy and filters so domain classes, resources and plugins are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   */
  MuleDeployableArtifactClassLoader build();

}
