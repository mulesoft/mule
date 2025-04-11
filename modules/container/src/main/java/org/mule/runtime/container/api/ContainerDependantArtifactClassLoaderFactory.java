/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;
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
  ArtifactClassLoader create(String artifactId, T descriptor, MuleContainerClassLoaderWrapper containerClassLoader)
      throws ArtifactClassloaderCreationException;

  /**
   * When using a Java version with JPMS support, will put the {@link ModuleLayer} where the given class is found as a parent of
   * the {@link ModuleLayer} from where the target {@link ClassLoader} will be obtained from.
   * <p>
   * If the Java version being used does not support JPMS, this does nothing.
   *
   * @param clazz the class to get the module layer from.
   */
  void setParentLayerFrom(Class clazz);

}
