/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;


import static java.util.Collections.swap;
import static java.util.stream.IntStream.range;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.manager.ServiceRegistry;

import java.util.List;
import java.util.OptionalInt;

/**
 * Default implementation of {@link ServiceDiscoverer}
 */
public class DefaultServiceDiscoverer implements ServiceDiscoverer {

  private final ServiceResolver serviceResolver;
  private final ServiceProviderDiscoverer serviceProviderDiscoverer;
  private static final String SCHEDULER_SERVICE_ARTIFACT_NAME = "Scheduler service";

  public DefaultServiceDiscoverer(ServiceProviderDiscoverer serviceProviderDiscoverer) {
    this(serviceProviderDiscoverer, new ReflectionServiceResolver(new ServiceRegistry()));
  }

  /**
   * Creates a new instance.
   *
   * @param serviceProviderDiscoverer discovers available service providers. Non null.
   * @param serviceResolver resolves dependencies on the discovered service providers. Non null.
   */
  public DefaultServiceDiscoverer(ServiceProviderDiscoverer serviceProviderDiscoverer, ServiceResolver serviceResolver) {
    checkArgument(serviceProviderDiscoverer != null, "serviceProviderDiscoverer cannot be null");
    checkArgument(serviceResolver != null, "serviceDependencyResolver cannot be null");
    this.serviceResolver = serviceResolver;
    this.serviceProviderDiscoverer = serviceProviderDiscoverer;
  }

  @Override
  public List<Service> discoverServices() throws ServiceResolutionError {
    try {
      final List<ServiceAssembly> assemblies = serviceProviderDiscoverer.discover();
      List<Service> resolvedServices = serviceResolver.resolveServices(assemblies);
      moveSchedulerServiceToEnd(resolvedServices);
      return resolvedServices;
    } catch (ServiceResolutionError e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceResolutionError(e.getMessage(), e);
    }
  }

  /**
   * Moves discovered 'Scheduler Service' to the end of the list, if present. Since the service manager
   * {@link org.mule.runtime.module.service.internal.manager.MuleServiceManager} stops all resolved services in the list order,
   * and the underlying order in which the provider obtains them is unreliable, the scheduler service has to be stopped last. This
   * prevents any scheduler from being force-shutdown when the service is being disposed, and lets the borrower do a prior clean
   * shutdown.
   *
   * @param discoveredServices raw services discovered
   */
  protected void moveSchedulerServiceToEnd(List<Service> discoveredServices) {
    // Find index of schedulerService
    OptionalInt indexOfSchedulerService = range(0, discoveredServices.size())
        .filter(index -> discoveredServices.get(index).getName().equals(SCHEDULER_SERVICE_ARTIFACT_NAME))
        .findFirst();
    if (indexOfSchedulerService.isPresent()) {
      swap(discoveredServices, indexOfSchedulerService.getAsInt(), discoveredServices.size() - 1);
    }
  }
}
