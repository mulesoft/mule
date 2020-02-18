/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ImmutableServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.io.InputStream;
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
   *                                    during classification process. The {@code artifactName} of each {@link ArtifactClassLoader} represents the
   *                                    {@value AetherClassPathClassifier#SERVICE_PROVIDER_CLASS_NAME} defined by the service in its
   *                                    {@value AetherClassPathClassifier##SERVICE_PROPERTIES_FILE_NAME}and it is used for instantiating the {@link ServiceProvider}.
   */
  public IsolatedServiceProviderDiscoverer(final List<ArtifactClassLoader> serviceArtifactClassLoaders) {
    checkNotNull(serviceArtifactClassLoaders, "serviceArtifactClassLoaders cannot be null");
    this.serviceArtifactClassLoaders = serviceArtifactClassLoaders;
  }

  @Override
  public List<ServiceAssembly> discover() throws ServiceResolutionError {
    List<ServiceAssembly> locators = new LinkedList<>();
    MuleServiceModelJsonSerializer serializer = new MuleServiceModelJsonSerializer();
    for (Object serviceArtifactClassLoader : serviceArtifactClassLoaders) {
      try {
        ClassLoader classLoader =
            (ClassLoader) serviceArtifactClassLoader.getClass().getMethod("getClassLoader").invoke(serviceArtifactClassLoader);

        MuleServiceModel serviceModel;
        try (InputStream descriptor = classLoader.getResourceAsStream("META-INF/mule-artifact/mule-artifact.json")) {
          serviceModel = serializer.deserialize(IOUtils.toString(descriptor));
        }

        for (MuleServiceContractModel contract : serviceModel.getContracts()) {
          ServiceProvider serviceProvider = instantiateServiceProvider(classLoader, contract.getServiceProviderClassName());
          locators.add(new ImmutableServiceAssembly(serviceModel.getName(), serviceProvider, classLoader,
                                                    loadClass(contract.getContractClassName(), getClass().getClassLoader())));
        }
      } catch (Exception e) {
        throw new IllegalStateException("Couldn't discover service from class loader: " + serviceArtifactClassLoader, e);
      }
    }

    return locators;
  }

  private ServiceProvider instantiateServiceProvider(ClassLoader classLoader, String className) throws ServiceResolutionError {
    Object reflectedObject;
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      reflectedObject = instantiateClass(className);
    } catch (Exception e) {
      throw new ServiceResolutionError(e.getMessage(), e);
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(format("Provided service class '%s' does not implement '%s'", className,
                                              ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }
}
