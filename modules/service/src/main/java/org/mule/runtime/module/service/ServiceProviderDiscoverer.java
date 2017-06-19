/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service;


import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.List;

/**
 * Discovers the {@link ServiceProvider} available in the container.
 */
public interface ServiceProviderDiscoverer {

  /**
   * Discovers available service providers.
   *
   * @return a non null list of {@link ServiceProvider} foiund in the container.
   * @throws ServiceResolutionError when a {@link ServiceProvider} cannot be properly instantiated.
   */
  List<Pair<ArtifactClassLoader, ServiceProvider>> discover() throws ServiceResolutionError;
}
