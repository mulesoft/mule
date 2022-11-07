/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.manager.ServiceRegistry;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;

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
  private final Injector containerInjector;

  public ReflectionServiceResolver(ServiceRegistry serviceRegistry, Injector containerInjector) {
    this.serviceRegistry = serviceRegistry;
    this.containerInjector = containerInjector;
  }

  @Override
  public List<Service> resolveServices(List<ServiceAssembly> assemblies) {
    List<Service> services = new ArrayList<>(assemblies.size());
    for (ServiceAssembly assembly : assemblies) {
      Service service = LazyServiceProxy.from(assembly, serviceRegistry, containerInjector);

      serviceRegistry.register(service, assembly.getServiceContract());
      services.add(service);
    }

    return services;
  }
}
