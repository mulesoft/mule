/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.service.discoverer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * {@link ServiceDiscoverer} implementation which performs an initial discovery by delegating into a
 * {@link ServiceProviderDiscoverer} but augments/overrides the found services with a fix list.
 *
 * @since 4.5.0
 */
public class OverridingServiceDiscoverer implements ServiceDiscoverer {

  private final Map<String, Service> overridesMap;
  private final ServiceDiscoverer delegate;

  public OverridingServiceDiscoverer(ServiceProviderDiscoverer serviceProviderDiscoverer, List<Service> overrides) {
    overridesMap = overrides.stream().collect(toMap(Service::getName, identity()));
    delegate = new DefaultServiceDiscoverer(serviceProviderDiscoverer) {

      @Override
      protected List<ServiceAssembly> discoverAssemblies() throws ServiceResolutionError {
        return super.discoverAssemblies().stream()
            .filter(assembly -> !overridesMap.containsKey(assembly.getName()))
            .collect(toList());
      }
    };
  }

  @Override
  public List<Service> discoverServices() throws ServiceResolutionError {
    List<Service> services = new LinkedList<>(delegate.discoverServices());
    services.addAll(overridesMap.values());

    return services;
  }
}
