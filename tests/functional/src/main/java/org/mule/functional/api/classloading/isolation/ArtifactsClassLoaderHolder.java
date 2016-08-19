/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.List;

/**
 * Holds {@link ClassLoader}s for application, services, plugins and container.
 *
 * @since 4.0
 */
public final class ArtifactsClassLoaderHolder {

  private ArtifactClassLoader containerClassLoader;
  private List<ArtifactClassLoader> servicesArtifactClassLoaders;
  private List<ArtifactClassLoader> pluginsArtifactClassLoaders;
  private ArtifactClassLoader applicationClassLoader;

  public ArtifactsClassLoaderHolder(ArtifactClassLoader containerClassLoader,
                                    List<ArtifactClassLoader> servicesArtifactClassLoaders,
                                    List<ArtifactClassLoader> pluginsArtifactClassLoaders,
                                    ArtifactClassLoader applicationClassLoader) {
    checkNotNull(containerClassLoader, "containerClassLoader cannot be null");
    checkNotNull(servicesArtifactClassLoaders, "servicesArtifactClassLoaders cannot be null");
    checkNotNull(pluginsArtifactClassLoaders, "pluginsArtifactClassLoaders cannot be null");
    checkNotNull(applicationClassLoader, "applicationClassLoader cannot be null");

    this.containerClassLoader = containerClassLoader;
    this.servicesArtifactClassLoaders = servicesArtifactClassLoaders;
    this.pluginsArtifactClassLoaders = pluginsArtifactClassLoaders;
    this.applicationClassLoader = applicationClassLoader;
  }

  public ArtifactClassLoader getContainerArtifactClassLoader() {
    return containerClassLoader;
  }

  public List<ArtifactClassLoader> getServicesArtifactClassLoaders() {
    return servicesArtifactClassLoaders;
  }

  public List<ArtifactClassLoader> getPluginsArtifactClassLoaders() {
    return pluginsArtifactClassLoaders;
  }

  public ArtifactClassLoader getApplicationArtifactClassLoader() {
    return applicationClassLoader;
  }

  /**
   * Loads the {@link Class} using the application {@link ArtifactClassLoader}.
   *
   * @param name {@link String} representing the name of the {@link Class} to be loaded.
   * @return the {@link Class} loaded with the application {@link ArtifactClassLoader}.
   * @throws ClassNotFoundException if the {@link Class} cannot be found.
   */
  public Class<?> loadClassWithApplicationClassLoader(String name) throws ClassNotFoundException {
    return applicationClassLoader.getClassLoader().loadClass(name);
  }
}
