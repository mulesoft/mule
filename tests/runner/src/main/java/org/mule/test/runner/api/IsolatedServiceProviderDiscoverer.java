/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.LinkedList;
import java.util.List;

/**
 * Discovers services artifacts using the {@link ArtifactClassLoader} already created.
 *
 * @since 4.0
 */
public class IsolatedServiceProviderDiscoverer implements ServiceProviderDiscoverer {

  private final List<ArtifactClassLoader> serviceArtifactClassLoaders;

  /**
   * Creates a new instance.
   *
   * @param serviceArtifactClassLoaders {@link List} of {@link ArtifactClassLoader}s created for services discovered
   *        during classification process. The {@code artifactName} of each {@link ArtifactClassLoader} represents the
   *        {@value AetherClassPathClassifier#SERVICE_PROVIDER_CLASS_NAME} defined by the service in its
   *        {@value AetherClassPathClassifier##SERVICE_PROPERTIES_FILE_NAME}and it is used for instantiating the {@link ServiceProvider}.
   */
  public IsolatedServiceProviderDiscoverer(final List<ArtifactClassLoader> serviceArtifactClassLoaders) {
    checkNotNull(serviceArtifactClassLoaders, "serviceArtifactClassLoaders cannot be null");
    this.serviceArtifactClassLoaders = serviceArtifactClassLoaders;
  }

  @Override
  public List<Pair<ArtifactClassLoader, ServiceProvider>> discover() throws ServiceResolutionError {
    List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new LinkedList<>();
    for (Object serviceArtifactClassLoader : serviceArtifactClassLoaders) {
      try {
        final ServiceProvider serviceProvider;
        String artifactName =
            (String) serviceArtifactClassLoader.getClass().getMethod("getArtifactId").invoke(serviceArtifactClassLoader);
        ClassLoader classLoader =
            (ClassLoader) serviceArtifactClassLoader.getClass().getMethod("getClassLoader").invoke(serviceArtifactClassLoader);

        serviceProvider = instantiateServiceProvider(classLoader,
                                                     artifactName);
        // TODO MULE-12254 - Remove null which is needed in order to avoid class cast exceptions
        serviceProviders.add(new Pair(null, serviceProvider));
      } catch (Exception e) {
        throw new IllegalStateException("Couldn't discover service from class loader: " + serviceArtifactClassLoader, e);
      }
    }

    return serviceProviders;
  }

  private ServiceProvider instantiateServiceProvider(ClassLoader classLoader, String className) throws ServiceResolutionError {
    Object reflectedObject;
    try {
      reflectedObject = withContextClassLoader(classLoader, () -> {
        try {
          return ClassUtils.instantiateClass(className);
        } catch (Exception e) {
          throw new MuleRuntimeException(createStaticMessage("Unable to create service from class: " + className), e);
        }
      });
    } catch (RuntimeException e) {
      throw new ServiceResolutionError(e.getMessage(), e);
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(format("Provided service class '%s' does not implement '%s'", className,
                                              ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }

}
