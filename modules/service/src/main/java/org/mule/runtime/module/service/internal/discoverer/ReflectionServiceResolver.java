/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.lang.reflect.Proxy.newProxyInstance;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceLocator;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;
import org.mule.runtime.module.service.internal.manager.ServiceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves {@link Service} instances given a set of {@link ServiceProvider} instances.
 * <p/>
 * Resolution process consist if find, for each service provider, all the service dependencies it has. Then, service providers
 * will be instantiated when the required set of dependencies is available.
 * <p/>
 * Resolution of services providers must be done always in the same order for the same set of providers to ensure a consistent
 * startup of the container.
 * <p/>
 * In case of a missing dependency, the resolution will fail and the container should not start.
 */
public class ReflectionServiceResolver implements ServiceResolver {

  private final ServiceRegistry serviceRegistry;

  public ReflectionServiceResolver(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public List<Service> resolveServices(List<ServiceLocator> serviceLocators) {

    List<Service> services = new ArrayList<>(serviceLocators.size());
    for (ServiceLocator locator : serviceLocators) {
      final Class<? extends Service> contract = locator.getServiceContract();
      Service service = (Service) newProxyInstance(contract.getClassLoader(),
                                                   new Class[] {contract, Startable.class, Stoppable.class},
                                                   new LazyServiceProxy(locator, serviceRegistry));

      serviceRegistry.register(service, locator);
      services.add(service);
    }

    return services;
  }
}
