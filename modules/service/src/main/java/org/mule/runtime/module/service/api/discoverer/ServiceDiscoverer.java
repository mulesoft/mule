/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.manager.ServiceRegistry;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.ReflectionServiceResolver;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Discovers the available services.
 */
@NoImplement
public interface ServiceDiscoverer {

  /**
   * Discover services.
   *
   * @return a non null list of {@link Service services} available in the container.
   * @throws ServiceResolutionError when a {@link Service} cannot be properly resolved during the discovery process.
   */
  List<Service> discoverServices() throws ServiceResolutionError;

  static ServiceDiscoverer create(ServiceProviderDiscoverer serviceProviderDiscoverer) {
    return new DefaultServiceDiscoverer(serviceProviderDiscoverer);
  }

  static ServiceDiscoverer create(ServiceProviderDiscoverer serviceProviderDiscoverer,
                                  ServiceRegistry serviceRegistry,
                                  BiFunction<Service, ServiceAssembly, Service> serviceWrapper) {
    return new DefaultServiceDiscoverer(serviceProviderDiscoverer,
                                        new ReflectionServiceResolver(serviceRegistry,
                                                                      null,
                                                                      serviceWrapper));
  }
}
