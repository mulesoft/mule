/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ImmutableServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
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
   * @param serviceArtifactClassLoaders {@link List} of {@link ArtifactClassLoader}s created for services discovered during
   *                                    classification process. The {@code artifactName} of each {@link ArtifactClassLoader}
   *                                    represents the {@value AetherClassPathClassifier#SERVICE_PROVIDER_CLASS_NAME} defined by
   *                                    the service in its {@value AetherClassPathClassifier##SERVICE_PROPERTIES_FILE_NAME}and it
   *                                    is used for instantiating the {@link ServiceProvider}.
   */
  public IsolatedServiceProviderDiscoverer(final List<ArtifactClassLoader> serviceArtifactClassLoaders) {
    requireNonNull(serviceArtifactClassLoaders, "serviceArtifactClassLoaders cannot be null");
    this.serviceArtifactClassLoaders = serviceArtifactClassLoaders;
  }

  @Override
  public List<ServiceAssembly> discover() throws ServiceResolutionError {
    List<ServiceAssembly> locators = new LinkedList<>();
    for (Object serviceArtifactClassLoader : serviceArtifactClassLoaders) {
      try {
        Object serviceDescriptor = serviceArtifactClassLoader.getClass()
            .getMethod("getArtifactDescriptor").invoke(serviceArtifactClassLoader);
        ClassLoader classLoader = (ClassLoader) serviceArtifactClassLoader.getClass()
            .getMethod("getClassLoader").invoke(serviceArtifactClassLoader);

        List<MuleServiceContractModel> contractModels = (List<MuleServiceContractModel>) serviceDescriptor.getClass()
            .getMethod("getContractModels").invoke(serviceDescriptor);
        String name = (String) serviceDescriptor.getClass()
            .getMethod("getName").invoke(serviceDescriptor);

        for (Object contract : contractModels) {
          String serviceProviderClassName = (String) contract.getClass()
              .getMethod("getServiceProviderClassName").invoke(contract);
          String contractClassName = (String) contract.getClass()
              .getMethod("getContractClassName").invoke(contract);

          ServiceProvider serviceProvider = instantiateServiceProvider(classLoader, serviceProviderClassName);
          locators.add(new ImmutableServiceAssembly(name, serviceProvider, classLoader,
                                                    loadClass(contractClassName, getClass().getClassLoader())));
        }
      } catch (Exception e) {
        throw new IllegalStateException("Couldn't discover service from class loader: " + serviceArtifactClassLoader, e);
      }
    }

    return locators;
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
