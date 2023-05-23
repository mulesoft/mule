/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.custom;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;

import java.util.stream.Stream;

@NoImplement
public interface ServiceConfigurator extends org.mule.runtime.api.config.custom.ServiceConfigurator {

  void configure(CustomizationService customizationService);

  @Override
  default void configure(org.mule.runtime.api.config.custom.CustomizationService customizationService) {
    configure((CustomizationService) customizationService);
  }

  /**
   * Looks up implementations of {@link ServiceConfigurator}.
   * 
   * @return the discovered {@link ServiceConfigurator}.
   */
  static Stream<ServiceConfigurator> lookupConfigurators() {
    return stream(((Iterable<ServiceConfigurator>) () -> load(ServiceConfigurator.class,
                                                              Service.class.getClassLoader())
                                                                  .iterator())
                                                                      .spliterator(),
                  false);
  }
}
