/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;

/**
 * Implementation of the ArtifactClassLoader interface for {@code mule-plugin}s, that manages shutdown listeners and has resource
 * releasers.
 */
public class MulePluginClassLoader extends MuleArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  /**
   * Constructs a new {@link MulePluginClassLoader} for the given URLs
   *
   * @param artifactId         artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls               the URLs from which to load classes and resources
   * @param parent             the parent class loader for delegation
   * @param lookupPolicy       policy used to guide the lookup process. Non null
   */
  public MulePluginClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                               ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
  }

}
