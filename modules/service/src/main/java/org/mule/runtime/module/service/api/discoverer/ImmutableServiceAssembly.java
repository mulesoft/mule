/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;

/**
 * Immutable implementation of {@link ServiceAssembly}
 *
 * @since 4.2
 */
public class ImmutableServiceAssembly implements ServiceAssembly {

  private final String name;
  private final ServiceProvider serviceProvider;
  private final ClassLoader classLoader;
  private final Class<? extends Service> serviceContract;

  /**
   * Creates a new instance
   *
   * @param name            the service's name
   * @param serviceProvider the {@link ServiceProvider}
   * @param classLoader     the service {@link ClassLoader}
   * @param serviceContract the {@link Service} contract that is being fulfilled.
   */
  public ImmutableServiceAssembly(String name, ServiceProvider serviceProvider,
                                  ClassLoader classLoader,
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
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public Class<? extends Service> getServiceContract() {
    return serviceContract;
  }
}
