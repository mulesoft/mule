/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;

import java.util.List;

/**
 * Discovers the available services.
 */
public interface ServiceDiscoverer {

  /**
   * Discover services.
   *
   * @return a non null list of {@link Service} availables in the container.
   * @throws ServiceResolutionError when a {@link Service} cannot be properly resolved during the discovery process.
   */
  List<Pair<ArtifactClassLoader, Service>> discoverServices() throws ServiceResolutionError;

  static ServiceDiscoverer create(ServiceProviderDiscoverer serviceProviderDiscoverer) {
    return new DefaultServiceDiscoverer(serviceProviderDiscoverer);
  }
}
