/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class ReflectionServiceResolverTestCase extends AbstractMuleTestCase {

  private final FooService fooService = mock(FooService.class);
  private final BarService barService = mock(BarService.class);
  private final ArtifactClassLoader fooServiceClassLoader = mock(ArtifactClassLoader.class);
  private final ArtifactClassLoader barServiceClassLoader = mock(ArtifactClassLoader.class);
  private final Pair<ArtifactClassLoader, ServiceProvider> fooServiceProvider = createFooServiceProvider(fooService);
  private final Pair<ArtifactClassLoader, ServiceProvider> barServiceProvider = createBarServiceProvider(barService);

  @Test
  public void resolvesIndependentServices() throws Exception {
    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProvider);
    serviceProviders.add(barServiceProvider);

    final ReflectionServiceResolver dependencyResolver =
        new ReflectionServiceResolver(mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS));

    final List<Pair<ArtifactClassLoader, Service>> servicesPairs = dependencyResolver.resolveServices(serviceProviders);

    List<Service> services = servicesPairs.stream().map(Pair::getSecond).collect(Collectors.toList());
    assertThat(services.size(), equalTo(2));
    assertThat(services, hasItem(fooService));
    assertThat(services, hasItem(barService));
  }

  @Test
  public void resolvesServiceOrderedDependency() throws Exception {
    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    serviceProviders.add(barServiceProvider);
    serviceProviders.add(fooServiceProvider);

    doServiceDependencyTest(fooService, fooServiceProvider, barService, serviceProviders);
  }

  @Test
  public void resolvesServiceDisorderedDependency() throws Exception {
    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProvider);
    serviceProviders.add(barServiceProvider);

    doServiceDependencyTest(fooService, fooServiceProvider, barService, serviceProviders);
  }

  private void doServiceDependencyTest(FooService fooService, Pair<ArtifactClassLoader, ServiceProvider> fooServiceProviderPair,
                                       BarService barService,
                                       List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders)
      throws ServiceResolutionError {
    final ServiceProviderResolutionHelper providerResolutionHelper =
        mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS);
    when(providerResolutionHelper.findServiceDependencies(fooServiceProviderPair.getSecond()))
        .thenReturn(singletonList(BarService.class));
    final ReflectionServiceResolver dependencyResolver = new ReflectionServiceResolver(providerResolutionHelper);

    final List<Pair<ArtifactClassLoader, Service>> servicePairs = dependencyResolver.resolveServices(serviceProviders);

    List<Service> services = servicePairs.stream().map(Pair::getSecond).collect(Collectors.toList());

    assertThat(services.size(), equalTo(2));
    assertThat(services.get(0), is(barService));
    assertThat(services.get(1), is(fooService));

    verify(providerResolutionHelper).injectInstance(same(fooServiceProviderPair.getSecond()), anyCollection());
  }

  @Test(expected = ServiceResolutionError.class)
  public void detectsUnresolvableServiceDependency() throws Exception {
    FooService fooService = mock(FooService.class);
    Pair<ArtifactClassLoader, ServiceProvider> fooServiceProviderPair = createFooServiceProvider(fooService);

    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new ArrayList<>();
    serviceProviders.add(fooServiceProviderPair);

    final ServiceProviderResolutionHelper providerResolutionHelper =
        mock(ServiceProviderResolutionHelper.class, RETURNS_DEEP_STUBS);
    when(providerResolutionHelper.findServiceDependencies(fooServiceProviderPair.getSecond()))
        .thenReturn(singletonList(BarService.class));
    final ReflectionServiceResolver dependencyResolver = new ReflectionServiceResolver(providerResolutionHelper);

    dependencyResolver.resolveServices(serviceProviders);
  }

  private Pair<ArtifactClassLoader, ServiceProvider> createBarServiceProvider(BarService barService) {
    ServiceProvider barServiceProvider = mock(ServiceProvider.class);
    final List<ServiceDefinition> barServices = new ArrayList<>();
    barServices.add(new ServiceDefinition(BarService.class, barService));
    when(barServiceProvider.providedServices()).thenReturn(barServices);
    return new Pair<>(barServiceClassLoader, barServiceProvider);
  }

  private Pair<ArtifactClassLoader, ServiceProvider> createFooServiceProvider(FooService fooService) {
    ServiceProvider fooServiceProvider = mock(ServiceProvider.class);
    final List<ServiceDefinition> fooServices = new ArrayList<>();
    fooServices.add(new ServiceDefinition(FooService.class, fooService));
    when(fooServiceProvider.providedServices()).thenReturn(fooServices);
    return new Pair<>(fooServiceClassLoader, fooServiceProvider);
  }

  public interface FooService extends Service {

  }

  public interface BarService extends Service {

  }
}
