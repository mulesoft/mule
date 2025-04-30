/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.service.config;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.service.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures available {@link Service} instances in an artifact's {@link CustomizationService} in order to resolve injectable
 * dependencies.
 */
public class ContainerServiceConfigurator implements ServiceConfigurator {

  private final List<Service> services;

  /**
   * Creates a new instance.
   *
   * @param services the services to configure
   */
  public ContainerServiceConfigurator(List<Service> services) {
    checkArgument(services != null, "services cannot be null");
    this.services = new ArrayList<>(services);
  }

  @Override
  public void configure(CustomizationService customizationService) {
    services.forEach(service -> {
      String name = service.getName();
      String contract = service.getContractName();
      if (!isEmpty(contract)) {
        name += " - " + contract;
      }
      customizationService.registerCustomServiceImpl(name, service, true);
    });
  }
}
