/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;


import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.List;

/**
 * Default implementation of {@link ServiceDiscoverer}
 */
public class DefaultServiceDiscoverer implements ServiceDiscoverer {

  private final ServiceResolver serviceResolver;
  private final ServiceProviderDiscoverer serviceProviderDiscoverer;

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
    final List<ServiceProvider> serviceProviders = serviceProviderDiscoverer.discover();
    return serviceResolver.resolveServices(serviceProviders);
  }
}
