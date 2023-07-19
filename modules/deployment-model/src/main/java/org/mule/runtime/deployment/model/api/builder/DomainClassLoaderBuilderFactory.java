/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.builder;

import org.mule.runtime.deployment.model.internal.domain.DefaultDomainClassLoaderBuilder;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;

/**
 * Factory to create instances of {@code DomainClassLoaderBuilder}.
 *
 * @since 4.5
 */
public class DomainClassLoaderBuilderFactory {

  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;

  /**
   * Creates an {@code DomainClassLoaderBuilderFactory} to create {@code DomainClassLoaderBuilder} instances.
   *
   * @param artifactClassLoaderResolver resolver that will be used to create the class loader. Non-null
   */
  public DomainClassLoaderBuilderFactory(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = artifactClassLoaderResolver;
  }

  /**
   * Creates a new {@code DomainClassLoaderBuilder} instance to create the domain artifact class loader.
   *
   * @return a {@code DomainClassLoaderBuilder} instance.
   */
  public DomainClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new DefaultDomainClassLoaderBuilder(artifactClassLoaderResolver);
  }

}
