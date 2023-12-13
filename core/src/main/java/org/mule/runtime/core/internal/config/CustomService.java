/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.config.custom.CustomizationService.ServiceInterceptor;

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

  private final String serviceId;
  private final Optional<Class> serviceClass;
  private final Optional<Consumer<ServiceInterceptor>> serviceImplInterceptorConsumer;

  /**
   * Creates a custom service from a class.
   *
   * @param serviceClass the service class.
   */
  public CustomService(String serviceId, Class serviceClass) {
    this.serviceId = serviceId;
    this.serviceClass = of(serviceClass);
    this.serviceImplInterceptorConsumer = empty();
  }

  /**
   * Creates a custom service from an implementation.
   *
   * @param serviceImpl the service implementation.
   */
  public CustomService(String serviceId, Object serviceImpl) {
    this.serviceId = serviceId;
    this.serviceImplInterceptorConsumer = of(serviceInterceptor -> serviceInterceptor.overrideServiceImpl(serviceImpl));
    this.serviceClass = empty();
  }

  /**
   * Creates a custom service from a {@link ServiceInterceptor} {@link Consumer}.
   *
   * @param serviceImplInterceptorConsumer the {@link Consumer} for the {@link ServiceInterceptor}.
   */
  public CustomService(String serviceId, Consumer<ServiceInterceptor> serviceImplInterceptorConsumer) {
    this.serviceId = serviceId;
    this.serviceImplInterceptorConsumer = of(serviceImplInterceptorConsumer);
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

  public <T> Optional<T> getServiceImpl(T defaultService) {
    if (!serviceImplInterceptorConsumer.isPresent()) {
      return empty();
    }

    DefaultServiceInterceptor<T> serviceInterceptor = new DefaultServiceInterceptor<>(serviceId, defaultService);
    serviceImplInterceptorConsumer.get().accept(serviceInterceptor);

    return serviceInterceptor.isRemove() ? empty() : ofNullable(serviceInterceptor.getNewServiceImpl());
  }

  private static class DefaultServiceInterceptor<T> implements ServiceInterceptor<T> {

    private final String serviceId;
    private final T serviceImpl;
    private T newServiceImpl;
    private boolean remove;

    public DefaultServiceInterceptor(String serviceId, T serviceImpl) {
      this.serviceId = serviceId;
      this.serviceImpl = serviceImpl;
    }

    @Override
    public Optional<T> getDefaultServiceImpl() {
      return ofNullable(serviceImpl);
    }

    @Override
    public void overrideServiceImpl(T newServiceImpl) {
      this.newServiceImpl = newServiceImpl;
    }

    @Override
    public void skip() {
      if (newServiceImpl != null) {
        throw new IllegalStateException(format("A 'newServiceImpl' is already present '%s' for service '%s' with default '%s'",
                                               newServiceImpl, serviceId, serviceImpl));
      }

      remove = true;
    }

    public T getNewServiceImpl() {
      if (remove) {
        throw new IllegalStateException(format("Service '%s' with default '%s' set to be removed, can't be overridden", serviceId,
                                               serviceImpl));
      }

      return newServiceImpl;
    }

    public boolean isRemove() {
      return remove;
    }

  }

}
