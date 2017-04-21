/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.module.service.LifecycleFilterServiceProxy.createLifecycleFilterServiceProxy;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.lifecycle.StartException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

/**
 * Service manager to use in the Mule container.
 */
public class MuleServiceManager implements ServiceManager {

  private static final Logger logger = getLogger(MuleServiceManager.class);

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
      final Service serviceProxy = createLifecycleFilterServiceProxy(registeredService);
      result.add(serviceProxy);
    }

    return unmodifiableList(result);
  }

  private void startServices() throws MuleException {
    for (Service service : registeredServices) {
      if (service instanceof Startable) {
        ClassLoader originalContextClassLoader = currentThread().getContextClassLoader();
        try {
          currentThread().setContextClassLoader(service.getClass().getClassLoader());
          ((Startable) service).start();
        } finally {
          currentThread().setContextClassLoader(originalContextClassLoader);
        }
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
