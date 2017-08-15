/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.LoaderNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a {@link DescriptorLoaderRepository} that uses a {@link ServiceRegistry} to detect available implementations of
 * {@link ClassLoaderModelLoader}
 */
public class ServiceRegistryDescriptorLoaderRepository implements DescriptorLoaderRepository {

  private final ServiceRegistry serviceRegistry;
  private final Class[] descriptorLoaderClasses = new Class[] {ClassLoaderModelLoader.class, BundleDescriptorLoader.class};
  private Map<Class, List<DescriptorLoader>> descriptorLoaders;

  /**
   * Creates a new repository
   *
   * @param serviceRegistry provides access to the {@link ClassLoaderModelLoader} that must be tracked on the repository. Non null
   */
  public ServiceRegistryDescriptorLoaderRepository(ServiceRegistry serviceRegistry) {
    checkArgument(serviceRegistry != null, "serviceRegistry cannot be null");

    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public synchronized <T extends DescriptorLoader> T get(String id, ArtifactType artifactType, Class<T> loaderClass)
      throws LoaderNotFoundException {
    if (descriptorLoaders == null) {
      initializeDescriptorLoaders();
    }

    DescriptorLoader descriptorLoader = null;
    List<DescriptorLoader> registeredDescriptorLoaders = descriptorLoaders.get(loaderClass);
    if (registeredDescriptorLoaders != null) {
      for (DescriptorLoader loader : registeredDescriptorLoaders) {
        if (loader.getId().equals(id) && loader.supportsArtifactType(artifactType)) {
          descriptorLoader = loader;
        }
      }
    }

    if (descriptorLoader == null) {
      throw new LoaderNotFoundException(noRegisteredLoaderError(id, loaderClass));
    }

    return (T) descriptorLoader;
  }

  protected static <T extends DescriptorLoader> String noRegisteredLoaderError(String id, Class<T> loaderClass) {
    return format("There is no loader with ID='%s' and type '%s'", id, loaderClass.getName());
  }

  private void initializeDescriptorLoaders() {
    synchronized (this) {
      if (descriptorLoaders == null) {
        descriptorLoaders = new HashMap<>();
        for (Class descriptorLoaderClass : descriptorLoaderClasses) {
          descriptorLoaders.put(descriptorLoaderClass, findBundleDescriptorLoaders(descriptorLoaderClass));
        }
      }
    }
  }

  private List<DescriptorLoader> findBundleDescriptorLoaders(Class<? extends DescriptorLoader> descriptorLoaderClass) {
    List<DescriptorLoader> descriptorLoaders = new ArrayList<>();
    Collection<? extends DescriptorLoader> providers =
        serviceRegistry.lookupProviders(descriptorLoaderClass, this.getClass().getClassLoader());

    for (DescriptorLoader loader : providers) {
      descriptorLoaders.add(loader);
    }

    return descriptorLoaders;
  }
}
