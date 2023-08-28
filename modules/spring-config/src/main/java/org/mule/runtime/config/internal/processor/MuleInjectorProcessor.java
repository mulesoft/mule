/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
