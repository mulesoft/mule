/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.module.launcher.service.LifecycleFilterServiceProxy.createServiceProxy;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service manager to use in the Mule container.
 */
public class MuleServiceManager implements ServiceManager {

  private static final Logger logger = LoggerFactory.getLogger(MuleServiceManager.class);

  private final ServiceDiscoverer serviceDiscoverer;
  private List<Service> registeredServices = new ArrayList<>();
  private List<Service> wrappedServices;

  /**
   * Creates a new instance.
   *
   * @param serviceDiscoverer container service discoverer. Non null.
   */
  public MuleServiceManager(ServiceDiscoverer serviceDiscoverer) {
    checkArgument(serviceDiscoverer != null, "serviceDiscoverer cannot be null");
    this.serviceDiscoverer = serviceDiscoverer;
  }

  @Override
  public void start() throws MuleException {
    File servicesFolder = getServicesFolder();
    if (!servicesFolder.exists()) {
      servicesFolder.mkdir();
    }

    try {
      registeredServices = serviceDiscoverer.discoverServices();
      wrappedServices = wrapServices(registeredServices);

      startServices();
    } catch (Exception e) {
      throw new StartException(e, this);
    }
  }

  private List<Service> wrapServices(List<Service> registeredServices) {
    final List<Service> result = new ArrayList<>(registeredServices.size());
    for (Service registeredService : registeredServices) {
      final Service serviceProxy = createServiceProxy(registeredService);
      result.add(serviceProxy);
    }

    return unmodifiableList(result);
  }

  private void startServices() throws MuleException {
    for (Service service : registeredServices) {
      if (service instanceof Startable) {
        ((Startable) service).start();
      }
    }
  }

  @Override
  public void stop() throws MuleException {
    for (int i = registeredServices.size() - 1; i >= 0; i--) {
      Service service = registeredServices.get(i);

      if (service instanceof Stoppable) {
        try {
          ((Stoppable) service).stop();
        } catch (Exception e) {
          logger.warn("Service {s} was not stopped properly: {s}", service.getName(), e.getMessage());
        }
      }
    }
  }

  @Override
  public List<Service> getServices() {
    return wrappedServices;
  }
}
