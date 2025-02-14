/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.internal.DefaultMuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

public interface IServiceClassLoaderFactory
    extends ArtifactClassLoaderFactory<ServiceDescriptor>, ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> {

  @Deprecated(since = "4.10.0")
  /**
   * @deprecated use {@link #create(String, ArtifactDescriptor, MuleContainerClassLoaderWrapper)} or
   *             {@link #create(String, ServiceDescriptor, ArtifactClassLoader)} instead.
   */
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                             ClassLoaderLookupPolicy lookupPolicy)
      throws ArtifactClassloaderCreationException;

  /**
   * Creates a {@link ClassLoader} from a given descriptor.
   *
   * @param artifactId           artifact unique ID.
   * @param descriptor           descriptor of the artifact owner of the created class loader.
   * @param containerClassLoader parent for the new artifact class loader.
   * @return a new class loader for described artifact.
   *
   * @since 4.6
   */
  default ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ArtifactClassLoader containerClassLoader)
      throws ArtifactClassloaderCreationException {
    return create(artifactId, descriptor, new DefaultMuleContainerClassLoaderWrapper(containerClassLoader));
  }
}
