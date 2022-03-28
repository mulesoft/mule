/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.activation.api.service.ServiceProviderDiscoverer;
import org.mule.runtime.module.artifact.activation.api.service.ServiceResolutionError;
import org.mule.runtime.module.artifact.activation.internal.service.discoverer.DefaultServiceDiscoverer;

import java.util.List;

/**
 * Discovers the available services.
 */
@NoImplement
@Deprecated
public interface ServiceDiscoverer extends org.mule.runtime.module.artifact.activation.api.service.ServiceDiscoverer {

  static ServiceDiscoverer create(ServiceProviderDiscoverer serviceProviderDiscoverer) {
    return new ServiceDiscoverer() {

      private final org.mule.runtime.module.artifact.activation.api.service.ServiceDiscoverer delegate =
          new DefaultServiceDiscoverer(serviceProviderDiscoverer);

      @Override
      public List<Service> discoverServices() throws ServiceResolutionError {
        return delegate.discoverServices();
      }
    };
  }
}
