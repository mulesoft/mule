/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.core.internal.lifecycle.InjectedDependenciesProvider;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;

public class DeclaredDependencyResolverTestCase {

  DeclaredDependencyResolver declaredDependencyResolver;
  private AbstractSpringRegistry springRegistry;
  private InjectedDependenciesProvider object;

  @Before
  public void setUp() throws Exception {
    springRegistry = mock(AbstractSpringRegistry.class);
    object = mock(InjectedDependenciesProvider.class);
    declaredDependencyResolver = new DeclaredDependencyResolver(springRegistry);
  }

  @Test
  @Description("check if getDeclaredDirectDependencies properly interacts with InjectedDependencyProvider")
  public void getDeclaredDirectDependenciesTest() {
    declaredDependencyResolver.getDeclaredDependencies(object);
    verify(object, atLeastOnce()).getInjectedDependencies();
  }
}
