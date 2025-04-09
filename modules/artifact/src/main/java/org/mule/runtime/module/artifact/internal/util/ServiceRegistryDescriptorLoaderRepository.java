/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.util;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.LoaderNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * Provides a {@link DescriptorLoaderRepository} that uses a {@link ServiceRegistry} to detect available implementations of
 * {@link ClassLoaderConfigurationLoader}
 */
public class ServiceRegistryDescriptorLoaderRepository implements DescriptorLoaderRepository, Disposable {

  private static final Logger LOGGER = getLogger(ServiceRegistryDescriptorLoaderRepository.class);

  private final Function<Class<? extends DescriptorLoader>, Stream<? extends DescriptorLoader>> serviceRegistry;
  private final Class[] descriptorLoaderClasses =
      new Class[] {ClassLoaderConfigurationLoader.class, BundleDescriptorLoader.class};
  private Map<Class, List<DescriptorLoader>> descriptorLoaders;

  public ServiceRegistryDescriptorLoaderRepository() {
    this(descriptorLoaderClass -> {
      return stream(((Iterable) () -> load(descriptorLoaderClass,
                                           ServiceRegistryDescriptorLoaderRepository.class.getClassLoader())
          .iterator())
          .spliterator(),
                    false);
    });
  }

  /**
   * Creates a new repository
   *
   * @param serviceRegistry provides access to the {@link ClassLoaderConfigurationLoader} that must be tracked on the repository.
   *                        Non null
   */
  public ServiceRegistryDescriptorLoaderRepository(Function<Class<? extends DescriptorLoader>, Stream<? extends DescriptorLoader>> serviceRegistry) {
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
    return unmodifiableList(serviceRegistry.apply(descriptorLoaderClass)
        .collect(toList()));
  }

  @Override
  public void dispose() {
    descriptorLoaders.forEach((descriptorLoaderClass, descriptorLoaders) -> descriptorLoaders
        .forEach(descriptorLoader -> disposeIfNeeded(descriptorLoader, LOGGER)));
  }

}
