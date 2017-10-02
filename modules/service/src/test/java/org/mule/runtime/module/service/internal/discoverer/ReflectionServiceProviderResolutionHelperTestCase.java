/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

public class ReflectionServiceProviderResolutionHelperTestCase extends AbstractMuleTestCase {

  private final ReflectionServiceProviderResolutionHelper providerResolutionHelper =
      new ReflectionServiceProviderResolutionHelper();


  @Test
  public void findsNoDependencies() throws Exception {
    final List<Class<? extends Service>> dependencies =
        providerResolutionHelper.findServiceDependencies(mock(ServiceProvider.class));
    assertThat(dependencies.size(), equalTo(0));
  }

  @Test
  public void findsDependencies() throws Exception {
    final List<Class<? extends Service>> dependencies =
        providerResolutionHelper.findServiceDependencies(new InjectableServiceProvider());
    assertThat(dependencies.size(), equalTo(1));
    assertThat(dependencies, hasItem(FooService.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsIfNonServiceDependencyIsFound() throws Exception {
    providerResolutionHelper.findServiceDependencies(new NoInjectableServiceProvider());
  }

  @Test
  public void injectsDependencies() throws Exception {
    final InjectableServiceProvider serviceProvider = new InjectableServiceProvider();
    List<ServiceDefinition> resolvedServices = new ArrayList<>();
    FooService fooService = mock(FooService.class);
    resolvedServices.add(new ServiceDefinition(FooService.class, fooService));
    providerResolutionHelper.injectInstance(serviceProvider, resolvedServices);

    assertThat(serviceProvider.fooService, is(fooService));
  }

  @Test(expected = ServiceResolutionError.class)
  public void detectsMissingDependency() throws Exception {
    final InjectableServiceProvider serviceProvider = new InjectableServiceProvider();

    providerResolutionHelper.injectInstance(serviceProvider, emptyList());
  }

  public interface FooService extends Service {

  }

  public static class InjectableServiceProvider implements ServiceProvider {

    @Inject
    FooService fooService;

    @Override
    public List<ServiceDefinition> providedServices() {
      return null;
    }
  }

  public static class NoInjectableServiceProvider implements ServiceProvider {

    @Inject
    String message;

    @Override
    public List<ServiceDefinition> providedServices() {
      return null;
    }
  }
}
