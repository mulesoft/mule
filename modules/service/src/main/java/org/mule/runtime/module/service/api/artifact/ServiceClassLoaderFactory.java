/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 * 
 * @deprecated from 4.9 use {@link ServiceModuleLayerFactory}
 */
// TODO W-12780081 - remove usages of deprecated creation metho
@Deprecated(since = "4.9")
public class ServiceClassLoaderFactory
    implements IServiceClassLoaderFactory {

  /**
   * @deprecated from 4.6 use {@link ServiceClassLoaderFactoryProvider} instead.
   */
  @Deprecated(since = "4.6")
  public ServiceClassLoaderFactory() {
    // Nothing to do
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated since 4.6, use {@link #create(String, ServiceDescriptor, MuleContainerClassLoaderWrapper)}.
   */
  @Override
  @Deprecated(since = "4.6")
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy)
      throws ArtifactClassloaderCreationException {
    return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                       lookupPolicy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor,
                                    MuleContainerClassLoaderWrapper containerClassLoader)
      throws ArtifactClassloaderCreationException {
    return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(),
                                       containerClassLoader.getContainerClassLoader().getClassLoader(),
                                       containerClassLoader.getContainerClassLoaderLookupPolicy());
  }

  @Override
  public void setParentLayerFrom(Class clazz) {
    // Nothing to do
  }

}
