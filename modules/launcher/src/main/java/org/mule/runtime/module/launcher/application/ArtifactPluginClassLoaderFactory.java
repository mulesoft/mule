/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

import java.net.URL;

/**
 * Creates {@link ArtifactClassLoader} for application or domain plugin descriptors.
 */
public class ArtifactPluginClassLoaderFactory implements ArtifactClassLoaderFactory<ArtifactPluginDescriptor> {

  /**
   * @param parent parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @return an {@link ArtifactClassLoader} for the given {@link ArtifactPluginDescriptor}
   */
  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ArtifactPluginDescriptor descriptor) {
    URL[] urls = new URL[descriptor.getRuntimeLibs().length + 1];
    urls[0] = descriptor.getRuntimeClassesDir();
    System.arraycopy(descriptor.getRuntimeLibs(), 0, urls, 1, descriptor.getRuntimeLibs().length);

    return new MuleArtifactClassLoader(descriptor.getName(), urls, parent.getClassLoader(), parent.getClassLoaderLookupPolicy());
  }
}
