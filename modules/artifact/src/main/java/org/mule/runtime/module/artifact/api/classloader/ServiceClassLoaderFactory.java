/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;

/**
 * Provides a way to create a {@link ClassLoader} for {@link Service}s.
 * 
 * @since 4.5
 */
public class ServiceClassLoaderFactory {

  private final ArtifactClassLoader containerClassLoader;

  public ServiceClassLoaderFactory(ArtifactClassLoader containerClassLoader) {
    this.containerClassLoader = containerClassLoader;
  }

  public MuleArtifactClassLoader createServiceClassLoader(String name, List<URL> urls) {
    MuleArtifactClassLoader serviceClassLoader =
        new MuleArtifactClassLoader(name,
                                    new ArtifactDescriptor(name),
                                    urls.toArray(new URL[0]),
                                    containerClassLoader.getClassLoader(),
                                    containerClassLoader.getClassLoaderLookupPolicy());


    return serviceClassLoader;
  }
}
