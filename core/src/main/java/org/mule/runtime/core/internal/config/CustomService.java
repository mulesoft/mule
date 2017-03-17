/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

/**
 * Defines a customization of a service.
 * <p>
 * A service can be specified by an implementation or a class that can be used to instantiate the implementation.
 *
 * @since 4.0
 */
public class CustomService {

  private Optional<Class> serviceClass;
  private Optional<Object> serviceImpl;

  /**
   * Creates a custom service from a class.
   *
   * @param serviceClass the service class.
   */
  public CustomService(Class serviceClass) {
    this.serviceClass = of(serviceClass);
    this.serviceImpl = empty();
  }

  /**
   * Creates a custom service from an implementation.
   *
   * @param serviceImpl the service implementation.
   */
  public CustomService(Object serviceImpl) {
    this.serviceImpl = of(serviceImpl);
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
    return serviceImpl;
  }

}
