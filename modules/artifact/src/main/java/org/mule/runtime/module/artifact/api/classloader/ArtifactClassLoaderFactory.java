/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Creates {@link ClassLoader} instances for Mule applications
 */
public interface ArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a classLoader from a given descriptor
   *
   * @param artifactId artifact unique ID
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @param parent parent for the new artifact classloader.
   * @param lookupPolicy lookup policy to use on the created classloader.
   * @return a new classLoader for described artifact
   */
  ArtifactClassLoader create(String artifactId, T descriptor, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy);

}
