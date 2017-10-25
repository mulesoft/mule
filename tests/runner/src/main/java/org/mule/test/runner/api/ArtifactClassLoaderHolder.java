/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.util.List;

/**
 * Holds {@link ClassLoader}s for application, plugins and container.
 *
 * @since 4.0
 */
public final class ArtifactClassLoaderHolder {

  private ArtifactClassLoader containerClassLoader;
  private List<ArtifactClassLoader> servicesArtifactClassLoaders;
  private List<ArtifactClassLoader> pluginsArtifactClassLoaders;
  private ArtifactClassLoader applicationClassLoader;

  public ArtifactClassLoaderHolder(ArtifactClassLoader containerClassLoader,
                                   List<ArtifactClassLoader> servicesArtifactClassLoaders,
                                   List<ArtifactClassLoader> pluginsArtifactClassLoaders,
                                   ArtifactClassLoader applicationClassLoader) {
    this.containerClassLoader = containerClassLoader;
    this.servicesArtifactClassLoaders = servicesArtifactClassLoaders;
    this.pluginsArtifactClassLoaders = pluginsArtifactClassLoaders;
    this.applicationClassLoader = applicationClassLoader;
  }

  public ArtifactClassLoader getContainerClassLoader() {
    return containerClassLoader;
  }

  public List<ArtifactClassLoader> getServicesClassLoaders() {
    return servicesArtifactClassLoaders;
  }

  public List<ArtifactClassLoader> getPluginsClassLoaders() {
    return pluginsArtifactClassLoaders;
  }

  public ArtifactClassLoader getApplicationClassLoader() {
    return applicationClassLoader;
  }

  /**
   * Loads the {@link Class} using the test runner {@link ArtifactClassLoader}.
   *
   * @param name {@link String} representing the name of the {@link Class} to be loaded.
   * @return the {@link Class} loaded with the application {@link ArtifactClassLoader}.
   * @throws ClassNotFoundException if the {@link Class} cannot be found.
   */
  public Class<?> loadClassWithATestRunnerClassLoader(String name) throws ClassNotFoundException {
    ArtifactClassLoader classLoader = getTestRunnerPluginClassLoader();
    return classLoader.getClassLoader().loadClass(name);
  }

  /**
   * @return the {@link ArtifactClassLoader} that corresponds to the test runner plugin
   */
  public ArtifactClassLoader getTestRunnerPluginClassLoader() {
    return pluginsArtifactClassLoaders.stream().filter(cl -> cl.getArtifactId().equals("Region/plugin/test-runner")).findFirst()
        .orElseThrow(() -> new IllegalStateException("No test runner plugin found"));
  }
}
