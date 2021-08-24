/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.CompositeModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.File;

/**
 * Provides a way to create {@link ArtifactClassLoader} for a Mule Runtime container.
 * 
 * @since 4.5
 */
public class ContainerClassLoaderProvider {

  /**
   * Creates a ModuleRepository based on the modules available on the provided {@code classloader}.
   * 
   * @param classLoader     where to look for modules.
   * @param temporaryFolder
   * @return
   */
  public static ModuleRepository createModuleRepository(ClassLoader classLoader, File temporaryFolder) {
    return new DefaultModuleRepository(new CompositeModuleDiscoverer(new JreModuleDiscoverer(),
                                                                     new ClasspathModuleDiscoverer(classLoader,
                                                                                                   temporaryFolder)));
  }

  /**
   * Creates the classLoader to represent the Mule container.
   *
   * @param moduleRepository  provides access to the modules available on the container. Non null.
   * @param parentClassLoader parent classLoader. Can be null.
   * @return a non null {@link ArtifactClassLoader} containing container code that can be used as parent classloader for other
   *         mule artifacts.
   */
  public static ArtifactClassLoader createContainerClassLoader(ModuleRepository moduleRepository, ClassLoader parentClassloader) {
    return new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(parentClassloader);

  }
}
