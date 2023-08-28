/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.discoverer;


import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.List;

/**
 * Discovers the {@link ServiceProvider} available in the container.
 */
@NoImplement
public interface ServiceProviderDiscoverer {

  /**
   * Discovers available service assemblies.
   *
   * @return a non null list of {@link ServiceAssembly} found in the container.
   * @throws ServiceResolutionError when a {@link ServiceProvider} cannot be properly instantiated.
   */
  List<ServiceAssembly> discover() throws ServiceResolutionError;
}
