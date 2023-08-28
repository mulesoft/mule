/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;

import java.util.List;

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
}
