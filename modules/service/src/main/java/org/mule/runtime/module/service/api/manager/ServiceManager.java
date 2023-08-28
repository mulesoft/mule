/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.manager;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.internal.manager.MuleServiceManager;

/**
 * Manages the lifecycle of the services available in the {@link ServiceRepository}
 */
@NoImplement
public interface ServiceManager extends Startable, Stoppable, ServiceRepository {

  static ServiceManager create(ServiceDiscoverer serviceDiscoverer) {
    return new MuleServiceManager(serviceDiscoverer);
  }
}
