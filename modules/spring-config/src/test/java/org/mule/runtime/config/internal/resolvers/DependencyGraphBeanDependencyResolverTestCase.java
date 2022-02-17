/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.resolvers;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;

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

  private DependencyGraphBeanDependencyResolver resolver;
  private ConfigurationDependencyResolver configurationDependencyResolver;
  private DeclaredDependencyResolver declaredDependencyResolver;
  private AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private AbstractSpringRegistry springRegistry;

  @Before
  public void setUp() throws Exception {
    configurationDependencyResolver = mock(ConfigurationDependencyResolver.class);
    declaredDependencyResolver = mock(DeclaredDependencyResolver.class);
    autoDiscoveredDependencyResolver = mock(AutoDiscoveredDependencyResolver.class);
    springRegistry = mock(AbstractSpringRegistry.class);
    resolver = new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                                         autoDiscoveredDependencyResolver, springRegistry);
  }

  @Test
  @Description("Check if getDirectBeanDependencies calls three different resolvers")
  public void getDirectBeanDependenciesTest() {
    when(springRegistry.get("component")).thenReturn(new DefaultStreamingManager());

    Object currentObject = springRegistry.get("component");

    resolver.getTransitiveDependencies("component", 5);
    verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies(("component"));
    verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
    verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies("component");
  }

  @Test
  @Description("Delegate dependency resolution to autoDiscoveredDependencyResolver")
  public void addAutoDiscoveredDependenciesTest() {
    when(springRegistry.get("component")).thenReturn(new DefaultStreamingManager());

    resolver.getTransitiveDependencies("component", 5);
    verify(autoDiscoveredDependencyResolver, times(1)).getAutoDiscoveredDependencies(("component"));
  }

  @Test
  @Description("Delegate dependency resolution to declaredDependencyResolver.")
  public void addDirectDeclaredDependenciesTest() {
    when(springRegistry.get("component")).thenReturn(new DefaultStreamingManager());

    Object currentObject = springRegistry.get("component");

    resolver.getTransitiveDependencies("component", 5);
    verify(declaredDependencyResolver, times(1)).getDeclaredDependencies(currentObject);
  }

  @Test
  @Description("Delegate dependency resolution to configurationDependencyResolver.")
  public void addDirectConfigurationDependenciesTest() {
    when(springRegistry.get("component")).thenReturn(new DefaultStreamingManager());

    resolver.getTransitiveDependencies("component", 5);
    verify(configurationDependencyResolver, times(1)).getDirectComponentDependencies("component");
  }

}
