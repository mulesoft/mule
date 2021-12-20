/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringRegistry extends AbstractSpringRegistry {

  private final BeanDependencyResolver beanDependencyResolver;

  private ApplicationContext baseApplicationContext;

  public SpringRegistry(ApplicationContext baseApplicationContext,
                        ApplicationContext applicationContext,
                        MuleContext muleContext,
                        ConfigurationDependencyResolver dependencyResolver,
                        LifecycleInterceptor lifecycleInterceptor) {
    super(applicationContext, muleContext, lifecycleInterceptor);
    this.baseApplicationContext = baseApplicationContext;
    this.beanDependencyResolver = new DefaultBeanDependencyResolver(dependencyResolver, this);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    addBeanFactoryPostProcessor(createBeforeInitialisationRegisteredObjectsPostProcessor());

    // This is used to track the Spring context lifecycle since there is no way to confirm the lifecycle phase from the
    // application context
    springContextInitialised.set(true);

    super.doInitialise();
  }

  private BeanDefinitionRegistryPostProcessor createBeforeInitialisationRegisteredObjectsPostProcessor() {
    return new BeanDefinitionRegistryPostProcessor() {

      @Override
      public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        registeredBeanDefinitionsBeforeInitialization.entrySet().stream()
            .forEach(beanDefinitionEntry -> registry.registerBeanDefinition(beanDefinitionEntry.getKey(),
                                                                            beanDefinitionEntry.getValue()));
      }

      @Override
      public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // do nothing
      }
    };
  }

  @Override
  protected void disposeContext() {
    if (((ConfigurableApplicationContext) baseApplicationContext).isActive()) {
      ((ConfigurableApplicationContext) baseApplicationContext).close();
    }

    baseApplicationContext = null;
  }

  <T> Map<String, T> lookupEntriesForLifecycleIncludingAncestors(Class<T> type) {
    // respect the order in which spring had resolved the beans
    Map<String, T> objects = new LinkedHashMap<>();
    objects.putAll(internalLookupByTypeWithoutAncestorsAndObjectProviders(type, false, false, baseApplicationContext));
    objects.putAll(internalLookupByTypeWithoutAncestorsAndObjectProviders(type, false, false, getApplicationContext()));
    return objects;
  }

  @Override
  public BeanDependencyResolver getBeanDependencyResolver() {
    return beanDependencyResolver;
  }

}
