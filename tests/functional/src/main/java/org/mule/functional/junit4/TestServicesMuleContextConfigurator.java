/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.findImplementedInterfaces;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Register services implementations.
 */
public class TestServicesMuleContextConfigurator implements ServiceConfigurator {

  private final ServiceRepository serviceRepository;
  private List<Service> testServices;

  /**
   * Creates a new instance.
   *
   * @param serviceRepository contains available service instances. Non null.
   */
  public TestServicesMuleContextConfigurator(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    this.serviceRepository = serviceRepository;

    Set<Class> currentContracts = getRegisteredServiceContracts();
    testServices = asList(new SimpleUnitTestSupportSchedulerService());

    testServices.removeIf(s -> currentContracts.stream().anyMatch(c -> c.isInstance(s)));
  }

  private Set<Class> getRegisteredServiceContracts() {
    return serviceRepository.getServices().stream()
        .flatMap(s -> stream(findImplementedInterfaces(s.getClass()))
            .filter(type -> Service.class.isAssignableFrom(type)))
        .collect(toSet());
  }

  @Override
  public void configure(CustomizationService customizationService) {
    Map<String, Service> servicesByName = new HashMap<>();

    // First, load any services from the classpath. This will be mock/test implementations
    testServices.forEach(service -> servicesByName.put(getServiceName(service), service));

    // Then, lookup any actual service implementations and replace any mocked with these ones.
    serviceRepository.getServices().forEach(service -> servicesByName.put(getServiceName(service), service));

    servicesByName.entrySet().stream()
        .forEach(entry -> customizationService.registerCustomServiceImpl(entry.getKey(), entry.getValue()));
  }

  public String getServiceName(Service service) {
    String name = service.getName();
    String contract = service.getContractName();
    if (!isEmpty(contract)) {
      name += " - " + contract;
    }
    return name;
  }
}
