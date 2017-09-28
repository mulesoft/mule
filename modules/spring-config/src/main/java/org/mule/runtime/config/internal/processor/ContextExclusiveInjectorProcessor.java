/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import org.mule.runtime.core.api.Injector;

import java.beans.PropertyDescriptor;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;

/**
 * Specialization of {@link SelectiveInjectorProcessor} which only considers beans which are defined on a given
 * {@link ApplicationContext}. This is useful to avoid exceptions related to unsatisfied dependencies when using parent context
 * which also define a {@link AutowiredAnnotationBeanPostProcessor}
 *
 * @since 3.7.0
 */
public final class ContextExclusiveInjectorProcessor extends SelectiveInjectorProcessor {

  private ApplicationContext applicationContext;

  public ContextExclusiveInjectorProcessor(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Only returns {@code true} if {@code beanName} is a key currently registered in the {@link #applicationContext} or if
   * {@code beanName} equals the {@code bean} classname, which in spring jargon means that we're injecting a non registered object
   * (most likely through {@link Injector#inject(Object)}
   */
  @Override
  protected boolean shouldInject(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {
    return applicationContext.containsBean(beanName) || beanName.equals(bean.getClass().getName());
  }
}
