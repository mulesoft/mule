/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.manager.DefaultServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ServiceRegistryTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  private DefaultServiceRegistry serviceRegistry;
  private ServiceA serviceA;
  @Mock
  private ServiceProvider serviceProviderA;

  @Before
  public void before() {
    serviceRegistry = new DefaultServiceRegistry();
    serviceA = new ServiceA();
  }

  private void registerServiceA() {
    serviceRegistry.register(serviceA, ServiceA.class);
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

    var thrown =
        assertThrows(ServiceResolutionError.class, () -> serviceRegistry.inject(serviceProviderRequiredDependencyToServiceA));
    assertThat(thrown.getMessage(), containsString("Could not inject dependency on field "
        + "'" + ServiceProviderRequiredDependencyToServiceA.class.getName() + "#serviceA' of type '"
        + ServiceA.class.getName() + "'"));
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
