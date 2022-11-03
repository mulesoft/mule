/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.manager.DefaultServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class ServiceRegistryTestCase extends AbstractMuleTestCase {

  private final DefaultServiceRegistry serviceRegistry = new DefaultServiceRegistry();

  @Test
  public void injectsDependencies() throws Exception {
    FooService service = mockFooService();
    final InjectableServiceProvider serviceProvider = new InjectableServiceProvider();

    serviceRegistry.inject(serviceProvider);

    assertThat(serviceProvider.fooService, is(service));
  }

  private FooService mockFooService() {
    FooService service = mock(FooService.class);
    serviceRegistry.register(service, (Class) FooService.class);

    return service;
  }

  @Test(expected = ServiceResolutionError.class)
  public void detectsMissingDependency() throws Exception {
    final InjectableServiceProvider serviceProvider = new InjectableServiceProvider();
    serviceRegistry.inject(serviceProvider);
  }

  public interface FooService extends Service {

  }

  public static class InjectableServiceProvider implements ServiceProvider {

    @Inject
    private FooService fooService;

    @Override
    public ServiceDefinition getServiceDefinition() {
      return null;
    }
  }

  public static class NoInjectableServiceProvider implements ServiceProvider {

    @Inject
    private String message;

    @Override
    public ServiceDefinition getServiceDefinition() {
      return null;
    }
  }
}
