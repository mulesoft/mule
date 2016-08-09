/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.functional.junit4.runners.ArtifactClassLoaderAdapter;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.ServiceResolutionError;

import java.util.LinkedList;
import java.util.List;

/**
 * Discovers services artifacts using the {@link ArtifactClassLoader} already created.
 *
 * @since 4.0
 */
public class IsolatedServiceProviderDiscoverer implements ServiceProviderDiscoverer {

  private final List<ArtifactClassLoaderAdapter> serviceArtifactClassLoaderAdapters;

  /**
   * Creates a new instance.
   *
   * @param serviceArtifactClassLoaderAdapters list of classLoaders created for services already discovered during classification
   *        process artifactName will be the serviceProviderClassName used for instantiating the {@link ServiceProvider}
   */
  public IsolatedServiceProviderDiscoverer(final List<ArtifactClassLoaderAdapter> serviceArtifactClassLoaderAdapters) {
    this.serviceArtifactClassLoaderAdapters = serviceArtifactClassLoaderAdapters;
  }

  @Override
  public List<ServiceProvider> discover() throws ServiceResolutionError {
    List<ServiceProvider> serviceProviders = new LinkedList<>();
    for (ArtifactClassLoaderAdapter serviceArtifactClassLoader : serviceArtifactClassLoaderAdapters) {
      final ServiceProvider serviceProvider;
      serviceProvider = instantiateServiceProvider(serviceArtifactClassLoader.getClassLoader(),
                                                   serviceArtifactClassLoader.getArtifactName());
      serviceProviders.add(serviceProvider);
    }

    return serviceProviders;
  }

  private ServiceProvider instantiateServiceProvider(ClassLoader classLoader, String className) throws ServiceResolutionError {
    Object reflectedObject;
    try {
      reflectedObject = withContextClassLoader(classLoader, () -> {
        try {
          return instanciateClass(className);
        } catch (Exception e) {
          throw new MuleRuntimeException(createStaticMessage("Unable to create service from class: " + className), e);
        }
      });
    } catch (RuntimeException e) {
      throw new ServiceResolutionError(e.getMessage());
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(format("Provided service class '%s' does not implement '%s'", className,
                                              ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }

}
