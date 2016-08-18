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
import org.mule.functional.junit4.runners.ArtifactClassLoaderReflector;
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

  private final List<ArtifactClassLoaderReflector> serviceArtifactClassLoaderReflectors;

  /**
   * Creates a new instance.
   *
   * @param serviceArtifactClassLoaderReflectors {@link List} of {@link ArtifactClassLoaderHolder}s created for services discovered
   *        during classification process. The {@code artifactName} of each {@link ArtifactClassLoaderReflector} represents the
   *        {@value org.mule.functional.classloading.isolation.classification.DefaultClassPathClassifier#SERVICE_PROVIDER_CLASS_NAME}
   *        defined by the service in its
   *        {@value org.mule.functional.classloading.isolation.classification.DefaultClassPathClassifier#SERVICE_PROPERTIES_FILE_NAME}
   *        and it is used for instantiating the {@link ServiceProvider}.
   */
  public IsolatedServiceProviderDiscoverer(final List<ArtifactClassLoaderReflector> serviceArtifactClassLoaderReflectors) {
    this.serviceArtifactClassLoaderReflectors = serviceArtifactClassLoaderReflectors;
  }

  @Override
  public List<ServiceProvider> discover() throws ServiceResolutionError {
    List<ServiceProvider> serviceProviders = new LinkedList<>();
    for (ArtifactClassLoaderReflector serviceArtifactClassLoader : serviceArtifactClassLoaderReflectors) {
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
