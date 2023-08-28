/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Creates {@link ClassLoader} instances for artifacts that depend directly on the container.
 *
 * @since 4.5
 */
@NoImplement
public interface ContainerDependantArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a {@link ClassLoader} from a given descriptor.
   *
   * @param artifactId           artifact unique ID.
   * @param descriptor           descriptor of the artifact owner of the created class loader.
   * @param containerClassLoader parent for the new artifact class loader.
   * @return a new class loader for described artifact.
   */
  ArtifactClassLoader create(String artifactId, T descriptor, MuleContainerClassLoaderWrapper containerClassLoader);
}
