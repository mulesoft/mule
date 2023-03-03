/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.stream.Collectors.toList;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;

/**
 * Base {@link ArtifactClassLoader} implementation of deployable artifacts.
 *
 * @since 4.0
 */
@NoExtend
@NoInstantiate
public class MuleDeployableArtifactClassLoader extends MuleArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  /**
   * Creates a {@link MuleDeployableArtifactClassLoader} with the provided configuration.
   *
   * @param artifactId         artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance.
   * @param urls               the URLs from which to load classes and resources
   * @param parent             parent class loader in the hierarchy
   * @param lookupPolicy       policy for resolving classes and resources
   */
  public MuleDeployableArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls,
                                           ClassLoader parent,
                                           ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
  }

  /**
   * @deprecated Use
   *             {@link #MuleDeployableArtifactClassLoader(String, ArtifactDescriptor, URL[], ClassLoader, ClassLoaderLookupPolicy)}
   *             instead.
   */
  @Deprecated
  public MuleDeployableArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls,
                                           ClassLoader parent,
                                           ClassLoaderLookupPolicy lookupPolicy,
                                           List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
  }

  /**
   * Provides a {@link List} with the plugin class loaders.
   *
   * @return {@link List} of plugin class loaders.
   */
  public List<ArtifactClassLoader> getArtifactPluginClassLoaders() {
    return ((RegionClassLoader) getParent()).getArtifactPluginClassLoaders()
        .stream()
        .map(fcl -> (fcl instanceof FilteringArtifactClassLoader)
            ? ((FilteringArtifactClassLoader) fcl).getArtifactClassLoader()
            : fcl)
        .collect(toList());
  }

  @Override
  public void dispose() {
    super.dispose();
    if (isRegionClassLoaderMember(this)) {
      ((RegionClassLoader) this.getParent()).disposeFromOwnerClassLoader();
    }
  }

  protected static boolean isRegionClassLoaderMember(ClassLoader classLoader) {
    return !isRegionClassLoader(classLoader) && isRegionClassLoader(classLoader.getParent());
  }

  private static boolean isRegionClassLoader(ClassLoader classLoader) {
    return classLoader instanceof RegionClassLoader;
  }
}
