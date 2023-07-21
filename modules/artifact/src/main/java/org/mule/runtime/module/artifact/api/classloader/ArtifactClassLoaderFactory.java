/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Creates {@link ClassLoader} instances for Mule applications
 */
@NoImplement
public interface ArtifactClassLoaderFactory<T extends ArtifactDescriptor> {

  /**
   * Creates a classLoader from a given descriptor
   *
   * @param artifactId   artifact unique ID
   * @param descriptor   descriptor of the artifact owner of the created classloader
   * @param parent       parent for the new artifact classloader.
   * @param lookupPolicy lookup policy to use on the created classloader.
   * @return a new classLoader for described artifact
   */
  ArtifactClassLoader create(String artifactId, T descriptor, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy);

}
