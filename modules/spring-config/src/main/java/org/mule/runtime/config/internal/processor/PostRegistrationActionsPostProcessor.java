/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import static org.mule.runtime.config.internal.MuleArtifactContext.INNER_BEAN_PREFIX;

import org.mule.runtime.core.internal.registry.MuleRegistryHelper;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * A {@link BeanPostProcessor} which invokes {@link MuleRegistryHelper#postObjectRegistrationActions(Object)} after spring
 * finishes initialization over each object.
 *
 * @since 3.7.0
 */
public class PostRegistrationActionsPostProcessor implements BeanPostProcessor {

  private final MuleRegistryHelper registryHelper;
  private final Set<String> seenBeanNames = new HashSet<>();
  private final ConfigurableListableBeanFactory beanFactory;

  /***
   * @param registryHelper registry helper to delegate post processing of beans.
   * @param beanFactory the bean factory to validate the type of beans to post process
   */
  public PostRegistrationActionsPostProcessor(MuleRegistryHelper registryHelper, ConfigurableListableBeanFactory beanFactory) {
    this.registryHelper = registryHelper;
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  /**
   * Execute post process actions over global elements in the mule configuration.
   * <p>
   * For instance, when a global transformer is defined, the post processing of it's creation will add it to the transformation
   * service as part of the transformation graph.
   * <p>
   * This logic must not be applied to inner elements that are not named by the user or are instances that were created from
   * prototype bean definitions. This is to prevent beans created for a particular flow like Converters to be registered twice.
   *
   * @param bean the bean instance
   * @param beanName the bean name
   * @return the bean instance
   * @throws BeansException
   */
  // TODO MULE-9638 - remove check for duplicates. It should not happen anymore when old parsing mode is not used anymore.
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // No need to take into account FactoryBeans
    if (bean instanceof FactoryBean) {
      return bean;
    }
    // For now we don't process duplicate bean names. This is the case with <transformer ref="a"/> where the same bean is
    // registered twice by the old parsing mechanism.
    if (!beanName.startsWith(INNER_BEAN_PREFIX) && !seenBeanNames.contains(beanName)) {
      if (beanFactory.containsBeanDefinition(beanName) && beanFactory.getBeanDefinition(beanName).isPrototype()) {
        // prototypes are created for inner usage within flows and must not be post-processed
        return bean;
      }
      seenBeanNames.add(beanName);
      registryHelper.postObjectRegistrationActions(bean);
    }
    return bean;
  }
}
