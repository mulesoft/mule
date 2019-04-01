/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class OrderingServiceDiscovererWrapper implements ServiceDiscoverer {

  private static final String SCHEDULER_SERVICE_ARTIFACT_NAME = "Scheduler service";
  ServiceDiscoverer delegate;

  public OrderingServiceDiscovererWrapper(ServiceDiscoverer delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<Service> discoverServices() throws ServiceResolutionError {
    List<Service> discoveredServices = delegate.discoverServices();
    moveSchedulerServiceToEnd(discoveredServices);
    return discoveredServices;
  }

  protected void moveSchedulerServiceToEnd(List<Service> discoveredServices) throws ServiceResolutionError {
    // Find index of schedulerService
    OptionalInt indexOfSchedulerService = IntStream.range(0, discoveredServices.size())
        .filter(index -> discoveredServices.get(index).getName().equals(SCHEDULER_SERVICE_ARTIFACT_NAME))
        .findFirst();
    if (!indexOfSchedulerService.isPresent()) {
      throw new ServiceResolutionError("Could not found SchedulerService when reordering found services for start/stop priorities.");
    }
    Collections.swap(discoveredServices, indexOfSchedulerService.getAsInt(), discoveredServices.size() - 1);
  }
}
