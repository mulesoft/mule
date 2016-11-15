/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.temporary;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;

/**
 * Creates a class loader instance for a temporary artifact.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderFactory implements DeployableArtifactClassLoaderFactory<ArtifactDescriptor> {

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, ArtifactDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    return new MuleDeployableArtifactClassLoader(artifactId, descriptor, new URL[0], parent.getClassLoader(),
                                                 parent.getClassLoaderLookupPolicy(), artifactPluginClassLoaders);
  }

}
