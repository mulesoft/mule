/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

/**
 * Provides a way to create {@link ArtifactClassLoader} for a Mule Runtime container.
 * 
 * @since 4.5
 */
public class ContainerClassLoaderProvider {

  /**
   * Creates the classLoader to represent the Mule container, with this class classloader as the parent classloader.
   *
   * @param moduleRepository provides access to the modules available on the container. Non null.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository) {
    return createContainerClassLoader(moduleRepository, ContainerClassLoaderProvider.class.getClassLoader());
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param moduleRepository  provides access to the modules available on the container. Non null.
   * @param parentClassLoader parent classLoader. Can be null.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository, ClassLoader parentClassLoader) {
    return new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(parentClassLoader);
  }
}
