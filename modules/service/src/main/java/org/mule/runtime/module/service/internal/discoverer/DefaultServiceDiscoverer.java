/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;


import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.manager.ServiceRegistry;

import java.util.List;

/**
 * Default implementation of {@link ServiceDiscoverer}
 */
public class DefaultServiceDiscoverer implements ServiceDiscoverer {

  private final ServiceResolver serviceResolver;
  private final ServiceProviderDiscoverer serviceProviderDiscoverer;

  public DefaultServiceDiscoverer(ServiceProviderDiscoverer serviceProviderDiscoverer) {
    this(serviceProviderDiscoverer, new ReflectionServiceResolver(new ServiceRegistry(), null));
  }

  /**
   * Creates a new instance.
   *
   * @param serviceProviderDiscoverer discovers available service providers. Non null.
   * @param serviceResolver           resolves dependencies on the discovered service providers. Non null.
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
      final List<ServiceAssembly> assemblies = discoverAssemblies();
      return serviceResolver.resolveServices(assemblies);
    } catch (ServiceResolutionError e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceResolutionError(e.getMessage(), e);
    }
  }

  protected List<ServiceAssembly> discoverAssemblies() throws ServiceResolutionError {
    return serviceProviderDiscoverer.discover();
  }
}
