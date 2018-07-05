/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.internal.factories.ConstantFactoryBean;

import java.lang.reflect.Method;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.MethodParameter;

public class SalvameDeEsta extends ContextAnnotationAutowireCandidateResolver {

  @Override
  public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    RootBeanDefinition beanDefinition = null;
    if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
      beanDefinition = (RootBeanDefinition) bdHolder.getBeanDefinition();
    }

    if (beanDefinition == null) {
      return super.isAutowireCandidate(bdHolder, descriptor);
    }

    boolean match = isTypeMatch(bdHolder, beanDefinition, descriptor);
    if (match && descriptor != null) {
      match = checkQualifiers(bdHolder, descriptor.getAnnotations());
      if (match) {
        MethodParameter methodParam = descriptor.getMethodParameter();
        if (methodParam != null) {
          Method method = methodParam.getMethod();
          if (method == null || void.class == method.getReturnType()) {
            match = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
          }
        }
      }
    }
    return match;
  }

  private boolean isTypeMatch(BeanDefinitionHolder bdHolder, RootBeanDefinition beanDefinition, DependencyDescriptor descriptor) {
    boolean match = ConstantFactoryBean.class.isAssignableFrom(beanDefinition.getTargetType());
    if (!match) {
      return super.isAutowireCandidate(bdHolder, descriptor);
    }

    Object value = beanDefinition.getConstructorArgumentValues().getArgumentValue(0, Object.class).getValue();
    return descriptor.getDependencyType().isInstance(value);
  }
}
