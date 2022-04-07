/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
