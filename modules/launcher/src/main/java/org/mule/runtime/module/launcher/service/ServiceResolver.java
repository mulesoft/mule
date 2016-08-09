/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;


import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.List;

/**
 * Resolves resolves all the {@link Service} provided by the available {@link ServiceProvider}.
 */
public interface ServiceResolver {

  /**
   * Resolves the services instances provided by the given service providers.
   *
   * @param serviceProviders service providers to be resolved. Non null.
   * @return the list of resolved services, sorted by the dependency relationship, i.e., all the services required by a given
   *         service must be located in the list before the dependant service.
   * @throws ServiceResolutionError
   */
  List<Service> resolveServices(List<ServiceProvider> serviceProviders) throws ServiceResolutionError;
}
