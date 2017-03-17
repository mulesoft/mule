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
import org.mule.runtime.module.service.ServiceRepository;

/**
 * Register services implementations.
 */
public class TestServicesMuleContextConfigurator implements ServiceConfigurator {

  private final ServiceRepository serviceRepository;

  /**
   * Creates a new instance.
   *
   * @param serviceRepository contains available service instances. Non null.
   */
  public TestServicesMuleContextConfigurator(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    this.serviceRepository = serviceRepository;
  }

  @Override
  public void configure(CustomizationService customizationService) {
    serviceRepository.getServices()
        .forEach(service -> customizationService.registerCustomServiceImpl(service.getName(), service));
  }
}
