/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import java.beans.PropertyDescriptor;

import org.mule.runtime.config.internal.OptionalObjectsController;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * A {@link InstantiationAwareBeanPostProcessor} which suspends initialization and population of discarded objects and removes
 * them from the registry
 *
 * @since 3.7.0
 */
public class DiscardedOptionalBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

  private final OptionalObjectsController optionalObjectsController;
  private final DefaultListableBeanFactory beanFactory;

  public DiscardedOptionalBeanPostProcessor(OptionalObjectsController optionalObjectsController,
                                            DefaultListableBeanFactory beanFactory) {
    this.optionalObjectsController = optionalObjectsController;
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
    return null;
  }

  @Override
  public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
    return !optionalObjectsController.isDiscarded(beanName);
  }

  @Override
  public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
      throws BeansException {
    return optionalObjectsController.isDiscarded(beanName) ? null : pvs;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (optionalObjectsController.isDiscarded(beanName)) {
      if (beanFactory.containsBeanDefinition(beanName)) {
        beanFactory.removeBeanDefinition(beanName);
      }

      beanFactory.destroySingleton(beanName);
      return null;
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }
}
