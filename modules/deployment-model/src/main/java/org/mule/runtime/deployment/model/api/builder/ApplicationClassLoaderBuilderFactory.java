/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.builder;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.deployment.model.internal.application.DefaultApplicationClassLoaderBuilder;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;

/**
 * Factory to create instances of {@code ApplicationClassLoaderBuilder}.
 *
 * @since 4.5
 */
public class ApplicationClassLoaderBuilderFactory {

  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;

  /**
   * Creates an {@code ApplicationClassLoaderBuilderFactory} to create {@code ApplicationClassLoaderBuilder} instances.
   *
   * @param artifactClassLoaderResolver resolver that will be used to create the class loader. Non-null
   */
  public ApplicationClassLoaderBuilderFactory(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = artifactClassLoaderResolver;
  }

  /**
   * Creates a new {@code ApplicationClassLoaderBuilder} instance to create the application artifact class loader.
   *
   * @return a {@code ApplicationClassLoaderBuilder} instance.
   */
  public ApplicationClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new DefaultApplicationClassLoaderBuilder(artifactClassLoaderResolver);
  }

}
