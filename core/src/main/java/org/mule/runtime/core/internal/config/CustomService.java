/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.config.custom.CustomizationService.ServiceOverrider;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Defines a customization of a service.
 * <p>
 * A service can be specified by an implementation or a class that can be used to instantiate the implementation.
 *
 * @since 4.0
 */
public class CustomService {

  private Optional<Class> serviceClass;
  private Optional<Consumer<ServiceOverrider>> serviceImplOverriderConsumer;

  /**
   * Creates a custom service from a class.
   *
   * @param serviceClass the service class.
   */
  public CustomService(Class serviceClass) {
    this.serviceClass = of(serviceClass);
    this.serviceImplOverriderConsumer = empty();
  }

  /**
   * Creates a custom service from an implementation.
   *
   * @param serviceImpl the service implementation.
   */
  public CustomService(Object serviceImpl) {
    this.serviceImplOverriderConsumer = of(serviceOverrider -> serviceOverrider.override(serviceImpl));
    this.serviceClass = empty();
  }

  /**
   * @return the service class.
   */
  public Optional<Class> getServiceClass() {
    return serviceClass;
  }

  /**
   * @return the service implementation.
   */
  public Optional<Object> getServiceImpl() {
    return getServiceImpl(null);
  }

  public Optional<Object> getServiceImpl(Object defaultService) {
    if (!serviceImplOverriderConsumer.isPresent()) {
      return empty();
    }

    DefaultServiceOverrider serviceOverrider = new DefaultServiceOverrider(defaultService);
    serviceImplOverriderConsumer.get().accept(serviceOverrider);

    return serviceOverrider.isRemove() ? empty() : ofNullable(serviceOverrider.getOverrider());
  }

  private static class DefaultServiceOverrider implements ServiceOverrider {

    private final Object serviceImpl;
    private Object overrider;
    private boolean remove;

    public DefaultServiceOverrider(Object serviceImpl) {
      this.serviceImpl = serviceImpl;
    }

    @Override
    public Object getOverridee() {
      return serviceImpl;
    }

    @Override
    public void override(Object overrider) {
      this.overrider = overrider;
    }

    @Override
    public void remove() {
      if (overrider != null) {
        throw new IllegalStateException("An 'overrider' service is already present");
      }

      remove = true;
    }

    public Object getOverrider() {
      if (remove) {
        throw new IllegalStateException("Service set to be removed, can't be overridden");
      }

      return overrider;
    }

    public boolean isRemove() {
      return remove;
    }

  }

}
