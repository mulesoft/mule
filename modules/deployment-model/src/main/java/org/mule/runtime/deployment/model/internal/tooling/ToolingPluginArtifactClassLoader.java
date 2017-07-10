/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static java.lang.String.format;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * Tooling {@link ClassLoader} that will delegate every call to it's delegate (the specific plugin class loader under
 * {@link #delegatePluginClassLoader}, but when doing the {@link #dispose()} it will dispatch
 * to the {@link RegionClassLoader} pointed by {@link #regionClassLoader} that contains all the plugins in it.
 *
 * @since 4.0
 */
public class ToolingPluginArtifactClassLoader implements ArtifactClassLoader {

  private final RegionClassLoader regionClassLoader;
  private final ArtifactClassLoader delegatePluginClassLoader;

  /**
   * Generates an instance of an {@link ArtifactClassLoader} if the parametrized {@code regionClassLoader} does contain
   * within its {@link RegionClassLoader#getArtifactPluginClassLoaders()} the class loader responsible of handling the
   * {@code artifactPluginDescriptor}.
   *
   * @param regionClassLoader class loader used to execute the {@link #dispose()} properly.
   * @param artifactPluginDescriptor descriptor to look for within the {@link RegionClassLoader}.
   */
  public ToolingPluginArtifactClassLoader(RegionClassLoader regionClassLoader,
                                          ArtifactPluginDescriptor artifactPluginDescriptor) {
    this.regionClassLoader = regionClassLoader;
    this.delegatePluginClassLoader =
        getPluginArtifactClassLoader(artifactPluginDescriptor, regionClassLoader.getArtifactPluginClassLoaders());
  }

  /**
   * @param artifactPluginDescriptor to look for within the collection of {@link ArtifactClassLoader}s
   * @param artifactPluginClassLoaders plugins class loaders to look for, at least one of them must contain the {@code pluginDescriptor}.
   * @return the {@link ArtifactClassLoader} that was generated for the {@code artifactPluginDescriptor}
   * @throws PluginResolutionError if the plugin wasn't found in the collection of {@code artifactPluginClassLoaders}
   */
  private ArtifactClassLoader getPluginArtifactClassLoader(ArtifactPluginDescriptor artifactPluginDescriptor,
                                                           List<ArtifactClassLoader> artifactPluginClassLoaders) {
    return artifactPluginClassLoaders.stream()
        .filter(artifactClassLoader -> artifactClassLoader.getArtifactId()
            .endsWith(PLUGIN_CLASSLOADER_IDENTIFIER + artifactPluginDescriptor.getName()))
        .findFirst()
        .orElseThrow(() -> new PluginResolutionError(format("Cannot generate a tooling ClassLoader as the region ClassLoader is missing the plugin '%s'",
                                                            artifactPluginDescriptor.getName())));
  }

  @Override
  public String getArtifactId() {
    return delegatePluginClassLoader.getArtifactId();
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return delegatePluginClassLoader.getArtifactDescriptor();
  }

  @Override
  public URL findResource(String resource) {
    return delegatePluginClassLoader.findResource(resource);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return delegatePluginClassLoader.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return delegatePluginClassLoader.findLocalClass(name);
  }

  @Override
  public ClassLoader getClassLoader() {
    return delegatePluginClassLoader.getClassLoader();
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    delegatePluginClassLoader.addShutdownListener(listener);
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return delegatePluginClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return delegatePluginClassLoader.findLocalResource(resourceName);
  }

  /**
   * We want tooling believe the {@link ArtifactClassLoader} he's handling is the plugin's one, but we are actually shipping
   * more than that with the {@link RegionClassLoader}.
   * <p>
   * So, to avoid any leaks the {@link #dispose()} of the plugin's {@link ArtifactClassLoader} is actually the one for the
   * {@link RegionClassLoader}, which will eventually execute a {@link #dispose()} over the plugin's one.
   */
  @Override
  public void dispose() {
    regionClassLoader.dispose();
  }
}
