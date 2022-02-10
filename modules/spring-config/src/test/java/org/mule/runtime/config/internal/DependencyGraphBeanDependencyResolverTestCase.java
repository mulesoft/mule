/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.config.internal.resolvers.AutoDiscoveredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DeclaredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Issue("MULE-19984")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class DependencyGraphBeanDependencyResolverTestCase {

  // DependencyGraphBeanDependencyResolver resolver;
  // private ConfigurationDependencyResolver configurationDependencyResolver;
  // private DeclaredDependencyResolver declaredDependencyResolver;
  // private AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  // private AbstractSpringRegistry springRegistry;
  //
  // @Before
  // public void setUp() throws Exception {
  // configurationDependencyResolver = mock(ConfigurationDependencyResolver.class);
  // declaredDependencyResolver = mock(DeclaredDependencyResolver.class);
  // autoDiscoveredDependencyResolver = mock(AutoDiscoveredDependencyResolver.class);
  // springRegistry = mock(AbstractSpringRegistry.class);
  // resolver = new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
  // autoDiscoveredDependencyResolver, springRegistry);
  // }
  //
  // @Test
  // @Description("check if getDirectBeanDependencies calls three different resolvers")
  // public void getDirectBeanDependenciesTest() {
  // String beanName = "component";
  // Object currentObject = springRegistry.get(beanName);
  // resolver.getDirectBeanDependencies(beanName);
  // verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies((beanName));
  // verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
  // verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies(beanName);
  // }
  //
  // @Test
  // @Description("delegate dependency resolution to autoDiscoveredDependencyResolver")
  // public void addAutoDiscoveredDependenciesTest() {
  // String beanName = "component";
  // resolver.getDirectBeanDependencies(beanName);
  // verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies((beanName));
  // }
  //
  // @Test
  // @Description("delegate dependency resolution to declaredDependencyResolver")
  // public void addDirectDeclaredDependenciesTest() {
  // String beanName = "component";
  // Object currentObject = springRegistry.get(beanName);
  // resolver.getDirectBeanDependencies(beanName);
  // verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
  // }
  //
  // @Test
  // @Description("delegate dependency resolution to configurationDependencyResolver")
  // public void addDirectConfigurationDependenciesTest() {
  // String beanName = "component";
  // resolver.getDirectBeanDependencies(beanName);
  // verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies(beanName);
  // }



}
