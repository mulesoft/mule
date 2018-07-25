/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceLocator;

public class ImmutableServiceLocator implements ServiceLocator {

  private final String name;
  private final ServiceProvider serviceProvider;
  private final ArtifactClassLoader classLoader;
  private final Class<? extends Service> serviceContract;

  public ImmutableServiceLocator(String name, ServiceProvider serviceProvider,
                                 ArtifactClassLoader classLoader,
                                 Class<? extends Service> serviceContract) {
    this.name = name;
    this.serviceProvider = serviceProvider;
    this.classLoader = classLoader;
    this.serviceContract = serviceContract;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ServiceProvider getServiceProvider() {
    return serviceProvider;
  }

  @Override
  public ArtifactClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public Class<? extends Service> getServiceContract() {
    return serviceContract;
  }
}
