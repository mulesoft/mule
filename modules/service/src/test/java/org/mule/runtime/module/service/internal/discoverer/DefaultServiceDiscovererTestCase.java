/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
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
    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    when(serviceProviderDiscoverer.discover()).thenReturn(serviceProviders);
    final List<Pair<ArtifactClassLoader, Service>> expectedServices = new ArrayList<>();
    when(serviceResolver.resolveServices(serviceProviders)).thenReturn(expectedServices);

    final List<Pair<ArtifactClassLoader, Service>> services = serviceDiscoverer.discoverServices();

    assertThat(services, is(expectedServices));
  }

  @Test(expected = ServiceResolutionError.class)
  public void propagatesDiscoverErrors() throws Exception {
    when(serviceProviderDiscoverer.discover()).thenThrow(new ServiceResolutionError("Error"));

    serviceDiscoverer.discoverServices();
  }

  @Test(expected = ServiceResolutionError.class)
  public void propagatesServiceResolutionErrors() throws Exception {
    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    when(serviceProviderDiscoverer.discover()).thenReturn(serviceProviders);
    when(serviceResolver.resolveServices(serviceProviders)).thenThrow(new ServiceResolutionError("Error"));

    serviceDiscoverer.discoverServices();
  }
}
