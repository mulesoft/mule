/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import java.util.Optional;
import java.util.function.BiFunction;

public class CombinedService<T> extends CustomService<T> {

  private final CustomService<T> customService1;
  private final CustomService<T> customService2;
  private final BiFunction<T, T, T> combiner;

  public CombinedService(String serviceId, CustomService<T> customService1, CustomService<T> customService2,
                         boolean baseContext, BiFunction<T, T, T> combiner) {
    super(serviceId, interceptor -> {
    }, baseContext);
    this.customService1 = customService1;
    this.customService2 = customService2;
    this.combiner = combiner;
  }

  @Override
  public Optional<T> getServiceImpl(T defaultService) {
    Optional<T> opt1 = customService1.getServiceImpl(defaultService);
    Optional<T> opt2 = customService2.getServiceImpl(defaultService);
    return opt1.map(impl1 -> opt2.map(impl2 -> combiner.apply(impl1, impl2))).orElse(opt2);
  }
}
