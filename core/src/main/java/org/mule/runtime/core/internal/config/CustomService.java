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

  private final Optional<Class> serviceClass;
  private final Optional<Consumer<ServiceInterceptor>> serviceImplInterceptorConsumer;

  /**
   * Creates a custom service from a class.
   *
   * @param serviceClass the service class.
   */
  public CustomService(Class serviceClass) {
    this.serviceClass = of(serviceClass);
    this.serviceImplInterceptorConsumer = empty();
  }

  /**
   * Creates a custom service from an implementation.
   *
   * @param serviceImpl the service implementation.
   */
  public CustomService(Object serviceImpl) {
    this.serviceImplInterceptorConsumer = of(serviceInterceptor -> serviceInterceptor.newServiceImpl(serviceImpl));
    this.serviceClass = empty();
  }

  /**
   * Creates a custom service from a {@link ServiceInterceptor} {@link Consumer}.
   *
   * @param serviceImplInterceptorConsumer the {@link Consumer} for the {@link ServiceInterceptor}.
   */
  public CustomService(Consumer<ServiceInterceptor> serviceImplInterceptorConsumer) {
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

  public Optional<Object> getServiceImpl(Object defaultService) {
    if (!serviceImplInterceptorConsumer.isPresent()) {
      return empty();
    }

    DefaultServiceInterceptor serviceInterceptor = new DefaultServiceInterceptor(defaultService);
    serviceImplInterceptorConsumer.get().accept(serviceInterceptor);

    return serviceInterceptor.isRemove() ? empty() : ofNullable(serviceInterceptor.getNewServiceImpl());
  }

  private static class DefaultServiceInterceptor implements ServiceInterceptor {

    private final Object serviceImpl;
    private Object newServiceImpl;
    private boolean remove;

    public DefaultServiceInterceptor(Object serviceImpl) {
      this.serviceImpl = serviceImpl;
    }

    @Override
    public Optional<Object> getDefaultServiceImpl() {
      return ofNullable(serviceImpl);
    }

    @Override
    public void newServiceImpl(Object newServiceImpl) {
      this.newServiceImpl = newServiceImpl;
    }

    @Override
    public void skip() {
      if (newServiceImpl != null) {
        throw new IllegalStateException("A 'newServiceImpl' is already present");
      }

      remove = true;
    }

    public Object getNewServiceImpl() {
      if (remove) {
        throw new IllegalStateException("Service set to be removed, can't be overridden");
      }

      return newServiceImpl;
    }

    public boolean isRemove() {
      return remove;
    }

  }

}
