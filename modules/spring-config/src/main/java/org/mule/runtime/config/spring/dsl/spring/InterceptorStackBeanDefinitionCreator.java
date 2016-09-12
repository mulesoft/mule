/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.INTERCEPTOR_STACK_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.REFERENCE_ATTRIBUTE;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.interceptor.InterceptorStack;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;

/**
 * {@link BeanDefinitionCreator} for elements interceptor-stack.
 *
 * We need a custom bena definition creator since the same element is used for defining an interceptor stack and referencing an
 * interceptor stack.
 *
 * @since 4.0
 */
public class InterceptorStackBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (!componentModel.getIdentifier().equals(INTERCEPTOR_STACK_IDENTIFIER)) {
      return false;
    }
    if (componentModel.getParameters().containsKey(REFERENCE_ATTRIBUTE)) {
      componentModel.setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
    } else {
      ManagedList<Object> interceptorList = new ManagedList<>();
      componentModel.getInnerComponents().forEach(childComponent -> interceptorList.add(childComponent.getBeanDefinition()));
      componentModel.setBeanDefinition(rootBeanDefinition(InterceptorStack.class)
          .setScope(BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT)
          .addConstructorArgValue(interceptorList)
          .getBeanDefinition());
    }
    componentModel.setType(InterceptorStack.class);
    return true;
  }
}
