/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
class ServiceModuleLayerFactory extends ServiceClassLoaderFactory {

  /**
   * {@inheritDoc}
   *
   * @deprecated since 4.6, use {@link #create(String, ServiceDescriptor, MuleContainerClassLoaderWrapper)}.
   */
  @Override
  @Deprecated
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy) {
    // TODO W-12683074 implement this using ModuleLayers
    return super.create(artifactId, descriptor, parent, lookupPolicy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor,
                                    MuleContainerClassLoaderWrapper containerClassLoader) {
    // TODO W-12683074 implement this using ModuleLayers
    return super.create(artifactId, descriptor, containerClassLoader);
  }
}
