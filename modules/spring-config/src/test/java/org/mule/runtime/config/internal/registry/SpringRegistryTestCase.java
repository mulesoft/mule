/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(REGISTRY)
@Story(OBJECT_REGISTRATION)
public class SpringRegistryTestCase extends AbstractMuleTestCase {

  private ConfigurableApplicationContext appContext;

  private DefaultListableBeanFactory beanFactory;

  @Before
  public void setUp() {
    appContext = mock(ConfigurableApplicationContext.class);
    beanFactory = mock(DefaultListableBeanFactory.class);
    when(appContext.getBeanFactory()).thenReturn(beanFactory);
  }

  @Test
  @Issue("MULE-20042")
  public void unregisterBeanWhoseCreationFails() throws RegistrationException {
    SpringRegistry registry = new SpringRegistry(appContext, appContext,
                                                 mock(MuleContext.class), mock(ConfigurationDependencyResolver.class),
                                                 mock(LifecycleInterceptor.class));

    when(beanFactory.containsBeanDefinition("key")).thenReturn(true);
    when(appContext.getBean("key")).thenThrow(BeanCreationException.class);

    assertThat(registry.unregisterObject("key"), is(nullValue()));
    verify(beanFactory).removeBeanDefinition("key");
    verify(beanFactory).destroySingleton("key");
  }

}
