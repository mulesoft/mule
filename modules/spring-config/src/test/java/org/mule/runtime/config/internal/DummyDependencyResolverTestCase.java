/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.registry.SpringContextRegistry;

import static org.mockito.Mockito.*;


public class DummyDependencyResolverTestCase {

  DummyDependencyResolver resolver;
  private ConfigurationDependencyResolver configurationDependencyResolver;
  private DeclaredDependencyResolver declaredDependencyResolver;
  private AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private SpringContextRegistry springRegistry;

  @Before
  public void setUp() throws Exception {
    configurationDependencyResolver = Mockito.mock(ConfigurationDependencyResolver.class);
    declaredDependencyResolver = Mockito.mock(DeclaredDependencyResolver.class);
    autoDiscoveredDependencyResolver = Mockito.mock(AutoDiscoveredDependencyResolver.class);
    springRegistry = Mockito.mock(SpringContextRegistry.class);
    resolver = new DummyDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                           autoDiscoveredDependencyResolver, springRegistry);
  }

  @Test
  @Description("check if it calls three different resolvers")
  public void getDirectBeanDependenciesTest() {
    String beanName = "component";
    Object currentObject = springRegistry.get(beanName);
    resolver.getDirectBeanDependencies(beanName);
    verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies(("component"));
    verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
    verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies(beanName);
  }

  // todo: test other parts of the getDirectBeanDependencies except for the addDependencies
  @Test
  @Description("delegate dependency resolution to autoDiscoveredDependencyResolver")
  public void addAutoDiscoveredDependenciesTest() {
    String beanName = "component";
    resolver.getDirectBeanDependencies(beanName);
    verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies(("component"));
  }

  @Test
  @Description("delegate dependency resolution to declaredDependencyResolver")
  public void addDirectDeclaredDependenciesTest() {
    String beanName = "component";
    Object currentObject = springRegistry.get(beanName);
    resolver.getDirectBeanDependencies(beanName);
    verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
  }

  @Test
  @Description("delegate dependency resolution to configurationDependencyResolver")
  public void addDirectConfigurationDependenciesTest() {
    String beanName = "component";
    resolver.getDirectBeanDependencies(beanName);
    verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies(beanName);
  }



}
