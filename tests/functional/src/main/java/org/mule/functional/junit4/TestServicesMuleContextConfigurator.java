/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.module.service.api.manager.ServiceRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Register services implementations.
 */
public class TestServicesMuleContextConfigurator implements ServiceConfigurator {

  private final ServiceRepository serviceRepository;
  private Collection<Service> testServices;

  /**
   * Creates a new instance.
   *
   * @param serviceRepository contains available service instances. Non null.
   */
  public TestServicesMuleContextConfigurator(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    this.serviceRepository = serviceRepository;

    testServices =
        new SpiServiceRegistry().lookupProviders(Service.class, TestServicesMuleContextConfigurator.class.getClassLoader());
  }

  @Override
  public void configure(CustomizationService customizationService) {
    Map<String, Service> servicesByName = new HashMap<>();

    // First, load any services from the classpath. This will be mock/test implementations
    testServices.forEach(service -> servicesByName.put(service.getName(), service));

    // Then, lookup any actual service implementations and replace any mocked with these ones.
    serviceRepository.getServices().forEach(service -> servicesByName.put(service.getName(), service));

    servicesByName.values().forEach(service -> customizationService.registerCustomServiceImpl(service.getName(), service));
  }
}
