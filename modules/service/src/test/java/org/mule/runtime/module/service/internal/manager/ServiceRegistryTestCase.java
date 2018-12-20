/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.manager;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ImmutableServiceAssembly;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Test;

public class ServiceRegistryTestCase extends AbstractMuleTestCase {

  @Test
  public void injectServiceDependency() {
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    ServiceA serviceA = new ServiceA();
    serviceRegistry.register(serviceA, new ImmutableServiceAssembly(serviceA.getName(), null, this.getClass().getClassLoader(), ServiceA.class));

    serviceRegistry.inject();
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
    }/Users/guillermo.fernandes/Development/mule/core-tests/src/test/java/org/mule/runtime/core/internal/exception/OnErrorPropagateHandlerTestCase.java
  }

}
