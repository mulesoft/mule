/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ReflectionServiceResolverTestCase extends AbstractMuleTestCase {

  private final FooService fooService = mock(FooService.class);
  private final BarService barService = mock(BarService.class);
  private final ServiceProvider fooServiceProvider = createFooServiceProvider(fooService);
  private final ServiceProvider barServiceProvider = createBarServiceProvider(barService);

  @Test
  public void resolvesIndependentServices() throws Exception {
    final List<ServiceProvider> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProvider);
    serviceProviders.add(barServiceProvider);

    final ReflectionServiceResolver dependencyResolver =
        new ReflectionServiceResolver(mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS));

    final List<Service> services = dependencyResolver.resolveServices(serviceProviders);

    assertThat(services.size(), equalTo(2));
    assertThat(services, hasItem(fooService));
    assertThat(services, hasItem(barService));
  }

  @Test
  public void resolvesServiceOrderedDependency() throws Exception {
    final List<ServiceProvider> serviceProviders = new ArrayList<>();
    serviceProviders.add(barServiceProvider);
    serviceProviders.add(fooServiceProvider);

    doServiceDependencyTest(fooService, fooServiceProvider, barService, serviceProviders);
  }

  @Test
  public void resolvesServiceDisorderedDependency() throws Exception {
    final List<ServiceProvider> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProvider);
    serviceProviders.add(barServiceProvider);

    doServiceDependencyTest(fooService, fooServiceProvider, barService, serviceProviders);
  }

  private void doServiceDependencyTest(FooService fooService, ServiceProvider fooServiceProvider, BarService barService,
                                       List<ServiceProvider> serviceProviders)
      throws ServiceResolutionError {
    final ServiceProviderResolutionHelper providerResolutionHelper =
        mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS);
    when(providerResolutionHelper.findServiceDependencies(fooServiceProvider))
        .thenReturn(Collections.singletonList(BarService.class));
    final ReflectionServiceResolver dependencyResolver = new ReflectionServiceResolver(providerResolutionHelper);

    final List<Service> services = dependencyResolver.resolveServices(serviceProviders);

    assertThat(services.size(), equalTo(2));
    assertThat(services.get(0), is(barService));
    assertThat(services.get(1), is(fooService));

    verify(providerResolutionHelper).injectInstance(same(fooServiceProvider), anyCollection());
  }

  @Test(expected = ServiceResolutionError.class)
  public void detectsUnresolvableServiceDependency() throws Exception {
    FooService fooService = mock(FooService.class);
    ServiceProvider fooServiceProvider = createFooServiceProvider(fooService);

    final List<ServiceProvider> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProvider);

    final ServiceProviderResolutionHelper providerResolutionHelper =
        mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS);
    when(providerResolutionHelper.findServiceDependencies(fooServiceProvider))
        .thenReturn(Collections.singletonList(BarService.class));
    final ReflectionServiceResolver dependencyResolver = new ReflectionServiceResolver(providerResolutionHelper);

    dependencyResolver.resolveServices(serviceProviders);
  }

  private ServiceProvider createBarServiceProvider(BarService barService) {
    ServiceProvider barServiceProvider = mock(ServiceProvider.class);
    final List<ServiceDefinition> barServices = new ArrayList<>();
    barServices.add(new ServiceDefinition(BarService.class, barService));
    when(barServiceProvider.providedServices()).thenReturn(barServices);
    return barServiceProvider;
  }

  private ServiceProvider createFooServiceProvider(FooService fooService) {
    ServiceProvider fooServiceProvider = mock(ServiceProvider.class);
    final List<ServiceDefinition> fooServices = new ArrayList<>();
    fooServices.add(new ServiceDefinition(FooService.class, fooService));
    when(fooServiceProvider.providedServices()).thenReturn(fooServices);
    return fooServiceProvider;
  }

  public interface FooService extends Service {

  }

  public interface BarService extends Service {

  }
}
