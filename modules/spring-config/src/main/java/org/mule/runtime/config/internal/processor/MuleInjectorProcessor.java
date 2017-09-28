/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Specialization of {@link AutowiredAnnotationBeanPostProcessor} which acts as the default injection post processors to be used
 * in Mule.
 * <p/>
 * Particularly, it overrides {@link #postProcessMergedBeanDefinition(RootBeanDefinition, Class, String)} to be a no-op method.
 * This is because although that method might make sense for the use cases that Spring tackles, it prevents {@link Inject} to
 * works as expected when an application is part of a non default mule domain
 *
 * @since 3.7.0
 */
public class MuleInjectorProcessor extends AutowiredAnnotationBeanPostProcessor {

  /**
   * No-Op method
   */
  @Override
  public final void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {}
}
