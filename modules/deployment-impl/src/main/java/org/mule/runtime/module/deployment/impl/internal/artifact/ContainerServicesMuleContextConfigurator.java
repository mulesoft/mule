/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.CustomizationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.artifact.MuleContextServiceConfigurator;
import org.mule.runtime.module.service.ServiceRepository;

/**
 * Configures available {@link Service} instances in an artifact's {@link MuleContext} in order to resolve injectable
 * dependencies.
 */
public class ContainerServicesMuleContextConfigurator implements MuleContextServiceConfigurator {

  private final ServiceRepository serviceRepository;

  /**
   * Creates a new instance.
   *
   * @param serviceRepository contains available service instances. Non null.
   */
  public ContainerServicesMuleContextConfigurator(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    this.serviceRepository = serviceRepository;
  }

  @Override
  public void configure(CustomizationService customizationService) {
    serviceRepository.getServices()
        .forEach(service -> customizationService.registerCustomServiceImpl(service.getName(), service));
  }
}
