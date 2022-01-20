/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mockito.Mockito.times;

import org.mule.runtime.config.internal.registry.SpringContextRegistry;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AutoDiscoveredDependencyResolverTestCase {

  AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private SpringContextRegistry springRegistry;

  @Before
  public void setUp() throws Exception {
    springRegistry = Mockito.mock(SpringContextRegistry.class);
    autoDiscoveredDependencyResolver = new AutoDiscoveredDependencyResolver(springRegistry);
  }

  @Test
  @Description("check if getAutoDiscoveredDependency properly interacts with springRegistry")
  public void getAutoDiscoveredDependenciesTest() {
    String beanName = "component";
    autoDiscoveredDependencyResolver.getAutoDiscoveredDependencies(beanName);
    Mockito.verify(springRegistry, times(1)).getDependencies(beanName);
  }
}
