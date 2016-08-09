/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

/**
 * Creates an application plugin artifact.
 *
 * @since 4.0
 */
public interface ArtifactPluginFactory {

  /**
   * Creates an {@link ArtifactPlugin} along with its classloader. The classloader for the application plugin would be a child
   * classloader of the given parent and it will use the same {@link ClassLoaderLookupPolicy} as the parent.
   *
   * @param descriptor that defines the application plugin
   * @param parent {@link ArtifactClassLoader} to be used as parent classloader
   * @return an {@link ArtifactPlugin}
   */
  ArtifactPlugin create(ArtifactPluginDescriptor descriptor, ArtifactClassLoader parent);
}
