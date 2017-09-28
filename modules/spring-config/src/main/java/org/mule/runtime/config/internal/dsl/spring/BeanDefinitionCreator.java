/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Abstract construct of a chain of responsibility to create a {@link org.springframework.beans.factory.config.BeanDefinition}
 * from a {@code org.mule.runtime.config.dsl.model.ComponentModel}.
 *
 * @since 4.0
 */
abstract class BeanDefinitionCreator {

  private BeanDefinitionCreator next;

  /**
   * @param nextBeanDefinitionCreator next processor in the chain.
   */
  public void setNext(BeanDefinitionCreator nextBeanDefinitionCreator) {
    this.next = nextBeanDefinitionCreator;
  }

  /**
   * Will iterate over the chain of processors until there's one that handle the request by return true to {@code #handleRequest}.
   *
   * @param request
   */
  public final void processRequest(CreateBeanDefinitionRequest request) {
    if (handleRequest(request)) {
      return;
    }
    if (next != null) {
      next.processRequest(request);
    }
  }

  /**
   * Instances of {@code BeanDefinitionCreator} that will be responsible to create the {@code BeanDefinition} must return true to
   * this call, otherwise they must do nothing.
   *
   * @param createBeanDefinitionRequest the creation request.
   * @return true if it created the {@code BeanDefinition}, false otherwise.
   */
  abstract boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest);

  protected BeanDefinition getConvertibleBeanDefinition(Class<?> type, Object value, Optional<TypeConverter> converter) {
    if (converter.isPresent()) {
      return genericBeanDefinition(ConstantFactoryBean.class).addConstructorArgValue(converter.get().convert(value))
          .getBeanDefinition();
    } else {
      return genericBeanDefinition(type).addConstructorArgValue(value).getBeanDefinition();
    }
  }
}
