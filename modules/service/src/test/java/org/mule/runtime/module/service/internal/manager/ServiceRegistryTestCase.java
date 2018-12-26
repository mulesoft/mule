/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.manager;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ImmutableServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryTestCase extends AbstractMuleTestCase {

  private ServiceRegistry serviceRegistry;
  private ServiceA serviceA;
  @Mock
  private ServiceProvider serviceProviderA;
  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    serviceRegistry = new ServiceRegistry();
    serviceA = new ServiceA();
  }

  private void registerServiceA() {
    serviceRegistry.register(serviceA, new ImmutableServiceAssembly(serviceA.getName(), serviceProviderA,
                                                                    this.getClass().getClassLoader(), ServiceA.class));
  }

  @Test
  public void injectRequiredServiceDependency() throws ServiceResolutionError {
    registerServiceA();

    ServiceProviderRequiredDependencyToServiceA serviceProviderRequiredDependencyToServiceA =
        new ServiceProviderRequiredDependencyToServiceA();
    serviceRegistry.inject(serviceProviderRequiredDependencyToServiceA);

    assertThat(serviceProviderRequiredDependencyToServiceA.serviceA, is(notNullValue()));
    assertThat(serviceProviderRequiredDependencyToServiceA.serviceA, sameInstance(serviceA));
  }

  @Test
  public void missingRequiredServiceDependency() throws ServiceResolutionError {
    ServiceProviderRequiredDependencyToServiceA serviceProviderRequiredDependencyToServiceA =
        new ServiceProviderRequiredDependencyToServiceA();
    expectedException.expect(ServiceResolutionError.class);
    expectedException
        .expectMessage(containsString("Could not inject dependency on field serviceA of type " + ServiceA.class.getName()));
    serviceRegistry.inject(serviceProviderRequiredDependencyToServiceA);
  }

  @Test
  public void injectEmptyOptionalServiceDependency() throws ServiceResolutionError {
    ServiceProviderOptionalDependencyToServiceA serviceProviderOptionalDependencyToServiceA =
        new ServiceProviderOptionalDependencyToServiceA();
    serviceRegistry.inject(serviceProviderOptionalDependencyToServiceA);

    assertThat(serviceProviderOptionalDependencyToServiceA.serviceA, is(empty()));
  }

  @Test
  public void injectOptionalServiceDependency() throws ServiceResolutionError {
    registerServiceA();

    ServiceProviderOptionalDependencyToServiceA serviceProviderOptionalDependencyToServiceA =
        new ServiceProviderOptionalDependencyToServiceA();
    serviceRegistry.inject(serviceProviderOptionalDependencyToServiceA);

    assertThat(serviceProviderOptionalDependencyToServiceA.serviceA.isPresent(), is(true));
    assertThat(serviceProviderOptionalDependencyToServiceA.serviceA.get(), sameInstance(serviceA));
  }

  public class ServiceA implements Service {

    @Override
    public String getName() {
      return this.getClass().getSimpleName();
    }
  }

  public class ServiceProviderRequiredDependencyToServiceA implements ServiceProvider {

    @Inject
    ServiceA serviceA;

    @Override
    public ServiceDefinition getServiceDefinition() {
      return null;
    }
  }

  public class ServiceProviderOptionalDependencyToServiceA implements ServiceProvider {

    @Inject
    Optional<ServiceA> serviceA;

    @Override
    public ServiceDefinition getServiceDefinition() {
      return null;
    }
  }

}
