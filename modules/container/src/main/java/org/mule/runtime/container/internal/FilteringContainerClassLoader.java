/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Filtering artifact classLoader that to use as the parent classloader for all mule tops artifact (domains, server plugins, etc).
 * <p>
 * Differs from the base class is that exposes all the resources available in the delegate classLoader and the delegate's parent
 * classLoader.
 */
public class FilteringContainerClassLoader extends FilteringArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  /**
   * Creates a new instance
   *
   * @param containerClassLoader delegate classLoader. Not null.
   * @param filter filter used to determine which classes and resources are exported on the delegate classLoader.
   */
  public FilteringContainerClassLoader(ArtifactClassLoader containerClassLoader, ClassLoaderFilter filter) {
    super(containerClassLoader, filter);
  }

  @Override
  protected URL getResourceFromDelegate(ArtifactClassLoader artifactClassLoader, String name) {
    return artifactClassLoader.getClassLoader().getResource(name);
  }

  @Override
  protected Enumeration<URL> getResourcesFromDelegate(ArtifactClassLoader artifactClassLoader, String name) throws IOException {
    return artifactClassLoader.getClassLoader().getResources(name);
  }
}
