/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.manager;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.internal.manager.MuleServiceManager;

/**
 * Manages the lifecycle of the services avaialbe in the {@link ServiceRepository}
 */
public interface ServiceManager extends Startable, Stoppable, ServiceRepository {

  static ServiceManager create(ServiceDiscoverer serviceDiscoverer) {
    return new MuleServiceManager(serviceDiscoverer);
  }
}
