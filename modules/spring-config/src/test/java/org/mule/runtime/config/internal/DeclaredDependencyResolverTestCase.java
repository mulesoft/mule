/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.internal.registry.SpringContextRegistry;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DeclaredDependencyResolverTestCase {

  DeclaredDependencyResolver declaredDependencyResolver;
  private SpringContextRegistry springRegistry;
  private InjectedDependenciesProvider object;

  @Before
  public void setUp() throws Exception {
    springRegistry = Mockito.mock(SpringContextRegistry.class);
    object = Mockito.mock(InjectedDependenciesProvider.class);
    declaredDependencyResolver = new DeclaredDependencyResolver(springRegistry);
  }

  @Test
  @Description("check if getDeclaredDirectDependencies properly interacts with InjectedDependencyProvider")
  public void getDeclaredDirectDependenciesTest() {
    declaredDependencyResolver.getDeclaredDependencies(object);
    Mockito.verify(object, Mockito.atLeastOnce()).getInjectedDependencies();
  }
}
