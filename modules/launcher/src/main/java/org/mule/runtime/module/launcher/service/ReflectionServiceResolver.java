/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves {@link Service} instances given a set of {@link ServiceProvider} instances.
 * <p/>
 * Resolution process consist if find, for each service provider, all the service dependencies it has. Then, service providers
 * will be instantiated when the required set of dependencies is available.
 * <p/>
 * Resolution of services providers must be done always in the same order for the same set of providers to ensure a consistent
 * startup of the container.
 * <p/>
 * In case of a missing dependency, the resolution will fail and the container should not start.
 */
public class ReflectionServiceResolver implements ServiceResolver {

  private final ServiceProviderResolutionHelper serviceProviderResolutionHelper;

  /**
   * Creates a new instance.
   *
   * @param serviceProviderResolutionHelper utility used to process service providers. Non null.
   */
  public ReflectionServiceResolver(ServiceProviderResolutionHelper serviceProviderResolutionHelper) {
    checkArgument(serviceProviderResolutionHelper != null, "serviceProviderResolutionHelper cannot be null");
    this.serviceProviderResolutionHelper = serviceProviderResolutionHelper;
  }

  @Override
  public List<Service> resolveServices(List<ServiceProvider> serviceProviders) throws ServiceResolutionError {
    List<DependencyAwareServiceProvider> dependencyAwareServiceProviders =
        createDependencyAwareServiceProviders(serviceProviders);

    Map<Class<? extends Service>, ServiceDefinition> registeredServices = new LinkedHashMap<>();
    List<DependencyAwareServiceProvider> unresolvedServiceProviders = new LinkedList<>(dependencyAwareServiceProviders);
    List<DependencyAwareServiceProvider> resolvedServiceProviders = new LinkedList<>();

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedServiceProviders.size();

      List<DependencyAwareServiceProvider> pendingUnresolvedServices = new LinkedList<>();

      for (DependencyAwareServiceProvider dependencyAwareServiceProvider : unresolvedServiceProviders) {
        if (isResolvedService(dependencyAwareServiceProvider, registeredServices.values())) {
          serviceProviderResolutionHelper.injectInstance(dependencyAwareServiceProvider.serviceProvider,
                                                         registeredServices.values());
          for (ServiceDefinition serviceDefinition : dependencyAwareServiceProvider.providedServices()) {
            registeredServices.put(serviceDefinition.getServiceClass(), serviceDefinition);
          }

          resolvedServiceProviders.add(dependencyAwareServiceProvider);
        } else {
          pendingUnresolvedServices.add(dependencyAwareServiceProvider);
        }
      }

      // Will try to resolve the services that are still unresolved
      unresolvedServiceProviders = pendingUnresolvedServices;

      continueResolution = resolvedServiceProviders.size() > initialResolvedCount;
    }

    if (unresolvedServiceProviders.size() != 0) {
      throw new ServiceResolutionError("Unable to resolve core service dependencies: " + unresolvedServiceProviders);
    }

    return registeredServices.values().stream().map(s -> s.getService()).collect(Collectors.toList());
  }

  private List<DependencyAwareServiceProvider> createDependencyAwareServiceProviders(List<ServiceProvider> serviceProviders) {
    final List<DependencyAwareServiceProvider> result = new ArrayList<>(serviceProviders.size());

    for (ServiceProvider serviceProvider : serviceProviders) {
      result.add(new DependencyAwareServiceProvider(serviceProvider,
                                                    serviceProviderResolutionHelper.findServiceDependencies(serviceProvider)));
    }

    result.sort((ServiceProvider p1, ServiceProvider p2) -> p1.getClass().getName().compareTo(p2.getClass().getName()));

    return result;
  }

  private boolean isResolvedService(DependencyAwareServiceProvider dependencyAwareServiceProvider,
                                    Collection<ServiceDefinition> resolvedServices) {
    boolean resolvedCoreExtension = dependencyAwareServiceProvider.dependencies.isEmpty();

    if (!resolvedCoreExtension && satisfiedDependencies(dependencyAwareServiceProvider.dependencies, resolvedServices)) {
      resolvedCoreExtension = true;
    }

    return resolvedCoreExtension;
  }

  private boolean satisfiedDependencies(List<Class<? extends Service>> dependencies,
                                        Collection<ServiceDefinition> resolvedServices) {
    boolean resolvedDependency = true;

    for (Class dependency : dependencies) {
      resolvedDependency = false;
      for (ServiceDefinition registeredService : resolvedServices) {
        if (registeredService.getServiceClass().isAssignableFrom(dependency)) {
          resolvedDependency = true;
        }
      }

      if (!resolvedDependency) {
        break;
      }
    }

    return resolvedDependency;
  }

  private final class DependencyAwareServiceProvider implements ServiceProvider {

    private final ServiceProvider serviceProvider;
    private final List<Class<? extends Service>> dependencies;

    DependencyAwareServiceProvider(ServiceProvider serviceProvider, List<Class<? extends Service>> dependencies) {
      this.serviceProvider = serviceProvider;
      this.dependencies = dependencies;
    }

    @Override
    public List<ServiceDefinition> providedServices() {
      return serviceProvider.providedServices();
    }

    List<Class<? extends Service>> getDependencies() {
      return dependencies;
    }
  }
}
