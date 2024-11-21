/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 *
 * @deprecated since 4.8, use {@link IServiceClassLoaderFactory}
 */
@Deprecated
public class ServiceClassLoaderFactory
    implements ArtifactClassLoaderFactory<ServiceDescriptor>, ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> {

  @Override
  @Deprecated
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                             ClassLoaderLookupPolicy lookupPolicy)
      throws ArtifactClassloaderCreationException;

  @Override
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor,
                             MuleContainerClassLoaderWrapper containerClassLoader)
      throws ArtifactClassloaderCreationException;

  @Override
  void setParentLayerFrom(Class clazz);
}
