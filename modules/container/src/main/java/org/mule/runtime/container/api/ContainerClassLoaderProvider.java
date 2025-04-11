/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.util.Set;


/**
 * Provides a way to create {@link ArtifactClassLoader} for a Mule Runtime container.
 *
 * @since 4.5
 */
// TODO W-12780081 - migrate to use MuleContainerClassLoaderWrapper
public class ContainerClassLoaderProvider {

  /**
   * Creates the classLoader to represent the Mule container, with this class classloader as the parent classloader.
   *
   * @param moduleRepository provides access to the modules available on the container. Non-null.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository) {
    return createContainerClassLoader(moduleRepository, ContainerClassLoaderProvider.class.getClassLoader());
  }

  /**
   * Creates the classLoader to represent the Mule container, with this class classloader as the parent classloader.
   *
   * @param moduleRepository                      provides access to the modules available on the container. Non-null.
   * @param bootPackages                          provides a set of packages that define all the prefixes that must be loaded from
   *                                              the container classLoader without being filtered.
   * @param additionalExportedResourceDirectories provides a set of directories of resources that should be additionally exported.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository, Set<String> bootPackages,
                                                               Set<String> additionalExportedResourceDirectories) {
    return createContainerClassLoader(moduleRepository, ContainerClassLoaderProvider.class.getClassLoader(), bootPackages,
                                      additionalExportedResourceDirectories);
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param moduleRepository  provides access to the modules available on the container. Non-null.
   * @param parentClassLoader parent classLoader. Can be null.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository, ClassLoader parentClassLoader) {
    return new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(parentClassLoader)
        .getContainerClassLoader();
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param moduleRepository                      provides access to the modules available on the container. Non-null.
   * @param parentClassLoader                     parent classLoader. Can be null.
   * @param bootPackages                          provides a set of packages that define all the prefixes that must be loaded from
   *                                              the container classLoader without being filtered.
   * @param additionalExportedResourceDirectories provides a set of directories of resources that should be additionally exported.
   * @return an {@link ArtifactClassLoader} containing container code that can be used as parent classLoader for other Mule
   *         artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository, ClassLoader parentClassLoader,
                                                               Set<String> bootPackages,
                                                               Set<String> additionalExportedResourceDirectories) {
    return new ContainerClassLoaderFactory(moduleRepository, bootPackages, additionalExportedResourceDirectories)
        .createContainerClassLoader(parentClassLoader).getContainerClassLoader();
  }
}
