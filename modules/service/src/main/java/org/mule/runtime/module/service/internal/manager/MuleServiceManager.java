/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.manager;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.manager.ServiceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service manager to use in the Mule container.
 */
public class MuleServiceManager implements ServiceManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleServiceManager.class);
  private static final String SCHEDULER_SERVICE_ARTIFACT_ID = "Scheduler service";
  private static final String HTTP_SERVICE_ARTIFACT_PREFIX = "HTTP";

  private final ServiceDiscoverer serviceDiscoverer;
  private List<Service> services = new ArrayList<>();

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
      services = serviceDiscoverer.discoverServices();
      startServices();
    } catch (Exception e) {
      throw new StartException(e, this);
    }
  }

  private void startServices() throws MuleException {
    for (Service service : services) {
      startIfNeeded(service);
    }
  }

  /**
   * Stop all discovered services. In the case of 'HTTP' and 'Scheduler' service, if present, they should be stopped last and in
   * that order.
   *
   * @throws MuleException
   */
  @Override
  public void stop() throws MuleException {
    Service schedulerService = null, httpService = null;
    for (Service service : services) {
      if (service.getName().equals(SCHEDULER_SERVICE_ARTIFACT_ID)) {
        schedulerService = service;
      } else if (service.getName().contains(HTTP_SERVICE_ARTIFACT_PREFIX)) {
        httpService = service;
      } else {
        doStopService(service);
      }
    }
    if(httpService != null) doStopService(httpService);
    if(schedulerService != null) doStopService(schedulerService);
  }

  protected void doStopService(Service service) {
    try {
      stopIfNeeded(service);
    } catch (Exception e) {
      LOGGER.warn(format("Failed to stop service '%s': %s", service.getName(), e.getMessage()), e);
    }
  }

  @Override
  public List<Service> getServices() {
    return unmodifiableList(services);
  }
}
