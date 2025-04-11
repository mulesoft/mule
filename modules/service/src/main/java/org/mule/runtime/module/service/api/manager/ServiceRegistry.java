/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.manager;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.internal.manager.DefaultServiceRegistry;

import java.util.Collection;
import java.util.Optional;

/**
 * Allows to configure the services that a given Runtime will manage.
 *
 * @since 4.5
 */
public interface ServiceRegistry {

  /**
   * @return an implementation of {@link ServiceRegistry} that does dependency injection between the registered services.
   */
  static ServiceRegistry create() {
    return new DefaultServiceRegistry();
  }

  /**
   * Tracks the given {@code service}
   *
   * @param service          the {@link Service} to be tracked
   * @param serviceInterface the interface of the service to unregister, as declared in
   *                         {@link ServiceAssembly#getServiceContract()} on {@link #register(Service, ServiceAssembly)}.
   */
  <S extends Service> void register(S service, Class<? extends S> serviceContract);

  /**
   * Un-tracks the given {@code service}.
   *
   * @param <S>              the specific service interface
   * @param serviceInterface the interface of the service to unregister, as declared in
   *                         {@link ServiceAssembly#getServiceContract()} on {@link #register(Service, ServiceAssembly)}.
   */
  <S extends Service> void unregister(Class<? extends S> serviceInterface);

  /**
   * Gets the service implementation for the provided interface.
   *
   * @param <S>             the specific service interface
   * @param serviceContract the interface of the service to get, as declared in {@link ServiceAssembly#getServiceContract()} on
   *                        {@link #register(Service, ServiceAssembly)}.
   * @return a previously {@link #register(Service, ServiceAssembly) registered} service for the provided
   *         {@code serviceInterface}.
   */
  <S extends Service> Optional<S> getService(Class<? extends S> serviceInterface);

  /**
   * @return all previously {@link #register(Service, ServiceAssembly) registered} services.
   */
  Collection<Service> getAllServices();
}
