/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.test.discoverer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.ServiceResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class DefaultServiceDiscovererTestCase extends AbstractMuleTestCase {

  private final ServiceResolver serviceResolver = mock(ServiceResolver.class);
  private final ServiceProviderDiscoverer serviceProviderDiscoverer = mock(ServiceProviderDiscoverer.class);
  private final DefaultServiceDiscoverer serviceDiscoverer =
      new DefaultServiceDiscoverer(serviceProviderDiscoverer, serviceResolver);

  @Test
  public void discoversServices() throws Exception {
    final List<ServiceAssembly> serviceProviders = new ArrayList<>();
    when(serviceProviderDiscoverer.discover()).thenReturn(serviceProviders);

    final List<Service> expectedServices = new ArrayList<>();
    when(serviceResolver.resolveServices(serviceProviders)).thenReturn(expectedServices);

    final List<Service> services = serviceDiscoverer.discoverServices();

    assertThat(services, is(expectedServices));
  }

  @Test(expected = ServiceResolutionError.class)
  public void propagatesDiscoverErrors() throws Exception {
    when(serviceProviderDiscoverer.discover()).thenThrow(new ServiceResolutionError("Error"));

    serviceDiscoverer.discoverServices();
  }

  @Test(expected = ServiceResolutionError.class)
  public void propagatesServiceResolutionErrors() throws Exception {
    final List<ServiceAssembly> serviceProviders = new ArrayList<>();
    when(serviceProviderDiscoverer.discover()).thenReturn(serviceProviders);
    when(serviceResolver.resolveServices(serviceProviders)).thenThrow(new RuntimeException(new ServiceResolutionError("Error")));

    serviceDiscoverer.discoverServices();
  }
}
