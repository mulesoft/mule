/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

/**
 * Creates {@link ClassLoader} instances for Mule applications
 */
public interface ArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a classLoader from a given descriptor
   *
   * @param parent parent for the new artifact classloader.
   * @param descriptor descriptor of the artifact owner of the created classloader
   * @return a new classLoader for described artifact
   */
  ArtifactClassLoader create(ArtifactClassLoader parent, T descriptor);

}
