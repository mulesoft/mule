/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.List;

public class ContractAwareServiceProviderWrapper implements ServiceProvider {

  private final ServiceProvider delegate;
  private final List<Class<? extends Service>> contracts;

  public ContractAwareServiceProviderWrapper(ServiceProvider delegate, List<Class<? extends Service>> contracts) {
    this.delegate = delegate;
    this.contracts = contracts;
  }

  @Override
  public List<ServiceDefinition> providedServices() {
    return delegate.providedServices();
  }

  public boolean satisfies(Class<? extends Service> contract) {
    return contracts.stream().anyMatch(contract::isAssignableFrom);
  }
}
