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

import org.mule.runtime.config.internal.registry.AbstractSpringRegistry;
import org.mule.runtime.config.internal.resolvers.AutoDiscoveredDependencyResolver;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Issue("MULE-19984")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class AutoDiscoveredDependencyResolverTestCase {

  private AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
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
