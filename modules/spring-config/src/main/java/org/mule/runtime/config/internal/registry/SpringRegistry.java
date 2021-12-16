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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class SpringRegistry extends AbstractSpringRegistry {

  public static final String REGISTRY_ID = "org.mule.Registry.Spring";

  private ApplicationContext baseApplicationContext;

  // Registered objects before the spring registry has been initialised.
  private final Map<String, BeanDefinition> registeredBeanDefinitionsBeforeInitialization = new HashMap<>();

  public SpringRegistry(ApplicationContext baseApplicationContext,
                        ApplicationContext applicationContext,
                        MuleContext muleContext,
                        ConfigurationDependencyResolver dependencyResolver,
                        LifecycleInterceptor lifecycleInterceptor) {
    super(applicationContext, muleContext, lifecycleInterceptor);
    this.baseApplicationContext = baseApplicationContext;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    ((AbstractApplicationContext) getApplicationContext())
        .addBeanFactoryPostProcessor(createBeforeInitialisationRegisteredObjectsPostProcessor());

    super.doInitialise();
  }

  private BeanDefinitionRegistryPostProcessor createBeforeInitialisationRegisteredObjectsPostProcessor() {
    return new BeanDefinitionRegistryPostProcessor() {

      @Override
      public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        registeredBeanDefinitionsBeforeInitialization.entrySet().stream().forEach(beanDefinitionEntry -> {
          registry.registerBeanDefinition(beanDefinitionEntry.getKey(), beanDefinitionEntry.getValue());
        });
      }

      @Override
      public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // do nothing
      }
    };
  }

  @Override
  public void disposeContext() {
    super.disposeContext();

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

}
