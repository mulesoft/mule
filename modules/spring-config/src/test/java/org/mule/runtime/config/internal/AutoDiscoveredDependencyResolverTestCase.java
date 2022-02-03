/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;

public class AutoDiscoveredDependencyResolverTestCase {

  AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private AbstractSpringRegistry springRegistry;

  @Before
  public void setUp() throws Exception {
    springRegistry = mock(AbstractSpringRegistry.class);
    autoDiscoveredDependencyResolver = new AutoDiscoveredDependencyResolver(springRegistry);
  }

  @Test
  @Description("check if getAutoDiscoveredDependency properly interacts with springRegistry")
  public void getAutoDiscoveredDependenciesTest() {
    String beanName = "component";
    autoDiscoveredDependencyResolver.getAutoDiscoveredDependencies(beanName);
    verify(springRegistry, times(1)).getDependencies(beanName);
  }
}
