/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import java.util.List;

public class LazyServiceProviderWrapper implements ServiceProvider {

  private final LazyValue<ServiceProvider> delegate;

  public LazyServiceProviderWrapper(CheckedSupplier<ServiceProvider> delegateSupplier) {
    delegate = new LazyValue<>(delegateSupplier);
  }

  @Override
  public List<ServiceDefinition> providedServices() {
    return delegate.get().providedServices();
  }
}
