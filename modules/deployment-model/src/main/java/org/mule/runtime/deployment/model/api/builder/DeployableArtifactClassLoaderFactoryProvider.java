/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.builder;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import java.io.File;
import java.util.function.Function;

/**
 * Provides concrete implementations of {@link DeployableArtifactClassLoaderFactory}.
 * 
 * @since 4.5
 */
public class DeployableArtifactClassLoaderFactoryProvider {

  /**
   * Creates a new factory for {@link ClassLoader}s of Mule Applications.
   *
   * @param nativeLibsTempFolderChildFunction a function to determine the location of a temp dir to copy the native libs of the
   *                                          artifact to, based on the deployment name.
   */
  public static DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory(Function<String, File> nativeLibsTempFolderChildFunction) {
    return new MuleApplicationClassLoaderFactory(nativeLibsTempFolderChildFunction);
  }

  /**
   * Creates a new factory for {@link ClassLoader}s of Mule Domains.
   *
   * @param nativeLibsTempFolderChildFunction a function to determine the location of a temp dir to copy the native libs of the
   *                                          artifact to, based on the deployment name.
   */
  public static DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory(Function<String, File> nativeLibsTempFolderChildFunction) {
    return new DomainClassLoaderFactory(nativeLibsTempFolderChildFunction);
  }

  /**
   * Creates a new factory for {@link RegionClassLoader}s of Mule Plugins.
   *
   * @param moduleRepository provides access to the modules available on the container. Non null.
   */
  public static RegionPluginClassLoadersFactory regionPluginClassLoadersFactory(ModuleRepository moduleRepository) {
    return new DefaultRegionPluginClassLoadersFactory(new ArtifactPluginClassLoaderFactory(), moduleRepository);
  }
}
