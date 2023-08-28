/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

/**
 * Provides constants to be used internally for the generation of classloaders
 *
 * @since 4.5
 */
public class ArtifactClassLoaderResolverConstants {

  /**
   * A {@link ModuleRepository} for the modules available in the same classpath as this class.
   */
  public static final ModuleRepository MODULE_REPOSITORY =
      new DefaultModuleRepository(new ContainerModuleDiscoverer(ArtifactClassLoaderResolverConstants.class.getClassLoader()));

  /**
   * An {@link ArtifactClassLoader} for the Mule Container with the modules available in {@link #MODULE_REPOSITORY}.
   */
  public static final ArtifactClassLoader CONTAINER_CLASS_LOADER =
      createContainerClassLoader(MODULE_REPOSITORY);

}
